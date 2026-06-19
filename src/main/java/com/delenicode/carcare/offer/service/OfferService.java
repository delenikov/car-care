package com.delenicode.carcare.offer.service;


import com.delenicode.carcare.customer.exception.CustomerNotFoundException;
import com.delenicode.carcare.offer.dto.request.OfferPartRequest;
import com.delenicode.carcare.offer.dto.request.OfferRequest;
import com.delenicode.carcare.offer.dto.response.OfferResponse;
import com.delenicode.carcare.offer.event.OfferCreatedEvent;
import com.delenicode.carcare.offer.exception.InvalidOfferException;
import com.delenicode.carcare.offer.exception.OfferNotFoundException;
import com.delenicode.carcare.offer.mapper.OfferMapper;
import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.offer.model.OfferPart;
import com.delenicode.carcare.offer.model.OfferStatus;
import com.delenicode.carcare.offer.repository.OfferRepository;
import com.delenicode.carcare.vehicle.exception.VehicleNotFoundException;
import com.delenicode.carcare.vehicle.repository.VehicleRepository;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.customer.repository.CustomerRepository;
import com.delenicode.carcare.common.PageResponse;
import com.delenicode.carcare.loyalty.service.CustomerLoyaltyService;
import com.delenicode.carcare.notification.PdfService;
import com.delenicode.carcare.vehicle.model.Vehicle;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferService {
  private final OfferRepository offers;
  private final CustomerRepository customers;
  private final VehicleRepository vehicles;
  private final PdfService pdfService;
  private final CustomerLoyaltyService loyalty;
  private final ApplicationEventPublisher events;
  private final OfferPricingService pricing;
  private final OfferMapper mapper;
  private final OfferEmailRenderer emailRenderer;
  private final OfferDeliveryService deliveryService;

  @Transactional(readOnly = true)
  public PageResponse<OfferResponse> findAll(Pageable pageable) {
    Page<Long> page = offers.findPageIds(pageable);
    if (page.isEmpty()) {
      return PageResponse.from(page, List.of());
    }
    Map<Long, Offer> offersById = new LinkedHashMap<>();
    offers.findAllWithDetailsByIdIn(page.getContent()).forEach(offer -> offersById.put(offer.getId(), offer));
    List<OfferResponse> content = page.getContent().stream()
        .map(offersById::get)
        .filter(Objects::nonNull)
        .map(mapper::toResponse)
        .toList();
    return PageResponse.from(page, content);
  }

  @Transactional(readOnly = true)
  public OfferResponse findById(Long id) {
    return mapper.toResponse(offers.findByIdWithDetails(id).orElseThrow(() -> new OfferNotFoundException(id)));
  }

  @Transactional
  public OfferResponse create(OfferRequest request) {
    List<OfferPartRequest> requestedParts = pricing.requestedParts(request);
    pricing.validateParts(requestedParts);

    Customer customer = customers.findByIdAndDeletedFalse(request.customerId()).orElseThrow(() -> new CustomerNotFoundException(request.customerId()));
    BigDecimal partsCost = pricing.partsCost(requestedParts, request.partsCost());
    BigDecimal laborCost = pricing.laborCost(request.laborCost(), request.amount());
    BigDecimal subtotalAmount = pricing.money(partsCost.add(laborCost));
    BigDecimal discountPercent = pricing.money(loyalty.discountPercentForCustomer(customer.getId()));
    BigDecimal discountAmount = pricing.discountAmount(subtotalAmount, discountPercent);

    Offer offer = new Offer();
    offer.setCustomer(customer);
    offer.setVehicle(resolveVehicle(request.vehicleId(), customer));
    offer.setTitle(request.title());
    offer.setDescription(request.description());
    offer.setPartsCost(partsCost);
    offer.setLaborCost(laborCost);
    offer.setSubtotalAmount(subtotalAmount);
    offer.setDiscountPercent(discountPercent);
    offer.setDiscountAmount(discountAmount);
    offer.setAmount(pricing.money(subtotalAmount.subtract(discountAmount)));
    offer.setExpiresOn(request.expiresOn());
    offer.setStatus(OfferStatus.PENDING_DELIVERY);
    for (int index = 0; index < requestedParts.size(); index++) {
      addPart(offer, requestedParts.get(index), index);
    }

    Offer saved = offers.save(offer);
    log.info(
        "Offer created. Offer ID: {}. Customer ID: {}. Vehicle ID: {}. Part count: {}. Subtotal: {}. Discount percent: {}. Discount amount: {}. Total: {}. Status: {}",
        saved.getId(),
        customer.getId(),
        saved.getVehicle() == null ? null : saved.getVehicle().getId(),
        saved.getParts().size(),
        saved.getSubtotalAmount(),
        saved.getDiscountPercent(),
        saved.getDiscountAmount(),
        saved.getAmount(),
        saved.getStatus());
    log.info("Offer created event published. Offer ID: {}", saved.getId());
    events.publishEvent(new OfferCreatedEvent(saved.getId()));
    return mapper.toResponse(saved);
  }

  public OfferResponse send(Long id) {
    return deliveryService.deliver(id);
  }

  public byte[] exportPdf(Long id) {
    Offer offer = offers.findByIdWithDetails(id).orElseThrow(() -> new OfferNotFoundException(id));
    log.info("Offer PDF exported. Offer ID: {}. Customer ID: {}", offer.getId(), offer.getCustomer().getId());
    return pdfService.renderServiceSummary("Понуда за сервис: " + offer.getTitle(), emailRenderer.renderText(offer));
  }

  private Vehicle resolveVehicle(Long vehicleId, Customer customer) {
    if (vehicleId == null) {
      return null;
    }
    Vehicle vehicle = vehicles.findById(vehicleId)
        .filter(existing -> !existing.getCustomer().isDeleted())
        .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
    if (!Objects.equals(vehicle.getCustomer().getId(), customer.getId())) {
      throw new InvalidOfferException("Vehicle does not belong to customer");
    }
    return vehicle;
  }

  private void addPart(Offer offer, OfferPartRequest requestedPart, int position) {
    OfferPart part = new OfferPart();
    part.setOffer(offer);
    part.setName(requestedPart.name().trim());
    part.setPrice(pricing.money(requestedPart.price()));
    part.setPosition(position);
    offer.getParts().add(part);
  }
}
