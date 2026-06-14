package com.delenicode.carcare.loyalty;

import com.delenicode.carcare.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/loyalty-rules")
@PreAuthorize("hasRole('ADMIN')")
public class LoyaltyRuleController {
  private final LoyaltyRuleService rules;

  @GetMapping
  ApiResponse<List<LoyaltyRuleResponse>> all() {
    return ApiResponse.ok("Loyalty rules loaded", rules.findAll());
  }

  @PostMapping
  ApiResponse<LoyaltyRuleResponse> create(@Valid @RequestBody LoyaltyRuleRequest request) {
    return ApiResponse.ok("Loyalty rule created", rules.create(request));
  }
}
