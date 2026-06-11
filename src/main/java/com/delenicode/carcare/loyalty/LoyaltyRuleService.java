package com.delenicode.carcare.loyalty;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoyaltyRuleService {
  private final LoyaltyRuleRepository rules;

  @Transactional(readOnly = true)
  public List<LoyaltyRuleResponse> findAll() {
    return rules.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional
  public LoyaltyRuleResponse create(LoyaltyRuleRequest request) {
    LoyaltyRule rule = new LoyaltyRule();
    rule.setName(request.name());
    rule.setPointsPerCurrencyUnit(request.pointsPerCurrencyUnit());
    rule.setDiscountPercent(request.discountPercent());
    rule.setActive(request.active());
    return toResponse(rules.save(rule));
  }

  public LoyaltyRuleResponse toResponse(LoyaltyRule rule) {
    return new LoyaltyRuleResponse(rule.getId(), rule.getName(), rule.getPointsPerCurrencyUnit(), rule.getDiscountPercent(), rule.isActive());
  }
}
