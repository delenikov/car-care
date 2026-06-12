package com.delenicode.carcare.offer;

import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.notification.EmailService;
import com.delenicode.carcare.notification.PdfService;
import com.delenicode.carcare.vehicle.Vehicle;
import com.delenicode.carcare.vehicle.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfferService {
  private final OfferRepository offers;
  private final CustomerRepository customers;
  private final VehicleRepository vehicles;
  private final EmailService emailService;
  private final PdfService pdfService;

  @Transactional(readOnly = true)
  public List<OfferResponse> findAll() {
    return offers.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public OfferResponse findById(Long id) {
    return offers.findById(id).map(this::toResponse).orElseThrow(() -> new IllegalArgumentException("Offer not found"));
  }

  @Transactional
  public OfferResponse create(OfferRequest request) {
    BigDecimal partsCost = costOrZero(request.partsCost());
    BigDecimal laborCost = costOrFallback(request.laborCost(), request.amount());
    Offer offer = new Offer();
    offer.setCustomer(customers.findById(request.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found")));
    if (request.vehicleId() != null) {
      offer.setVehicle(vehicles.findById(request.vehicleId()).orElseThrow(() -> new IllegalArgumentException("Vehicle not found")));
    }
    offer.setTitle(request.title());
    offer.setDescription(request.description());
    offer.setPartsCost(partsCost);
    offer.setLaborCost(laborCost);
    offer.setAmount(partsCost.add(laborCost));
    offer.setExpiresOn(request.expiresOn());
    return toResponse(offers.save(offer));
  }

  @Transactional
  public OfferResponse send(Long id) {
    Offer offer = offers.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer not found"));
    emailService.send(offer.getCustomer().getEmail(), "Service quotation: " + offer.getTitle(), offerBody(offer));
    offer.setStatus(OfferStatus.SENT);
    return toResponse(offers.save(offer));
  }

  @Transactional(readOnly = true)
  public byte[] exportPdf(Long id) {
    Offer offer = offers.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer not found"));
    return pdfService.renderServiceSummary("Quotation: " + offer.getTitle(), offerBody(offer));
  }

  public OfferResponse toResponse(Offer offer) {
    Vehicle vehicle = offer.getVehicle();
    return new OfferResponse(offer.getId(), offer.getCustomer().getId(), vehicle == null ? null : vehicle.getId(), offer.getTitle(), offer.getDescription(), offer.getPartsCost(), offer.getLaborCost(), offer.getAmount(), offer.getExpiresOn(), offer.getStatus());
  }

  private String offerBody(Offer offer) {
    return "Parts cost: " + offer.getPartsCost() + "\nLabor cost: " + offer.getLaborCost() + "\nTotal cost: " + offer.getAmount() + "\n" + (offer.getDescription() == null ? "" : offer.getDescription());
  }

  private BigDecimal costOrZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private BigDecimal costOrFallback(BigDecimal value, BigDecimal fallback) {
    if (value != null) {
      return value;
    }
    return fallback == null ? BigDecimal.ZERO : fallback;
  }
}
