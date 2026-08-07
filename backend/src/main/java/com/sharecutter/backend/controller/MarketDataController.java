package com.sharecutter.backend.controller;

import com.sharecutter.backend.dto.marketdata.MarketPriceResponse;
import com.sharecutter.backend.dto.marketdata.MarketSymbolSearchResponse;
import com.sharecutter.backend.service.marketdata.MarketDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market-data")
public class MarketDataController {

    private final MarketDataService
            marketDataService;

    public MarketDataController(
            MarketDataService marketDataService
    ) {
        this.marketDataService =
                marketDataService;
    }

    @GetMapping("/search")
    public ResponseEntity<
            List<MarketSymbolSearchResponse>>
    searchSymbols(
            @RequestParam String query,
            @RequestParam(
                    required = false
            )
            Integer limit
    ) {
        return ResponseEntity.ok(
                marketDataService.searchSymbols(
                        query,
                        limit
                )
        );
    }

    @GetMapping("/price")
    public ResponseEntity<MarketPriceResponse>
    getLatestPrice(
            @RequestParam String symbol,
            @RequestParam(
                    required = false
            )
            String exchange
    ) {
        return ResponseEntity.ok(
                marketDataService.getLatestPrice(
                        symbol,
                        exchange
                )
        );
    }
}