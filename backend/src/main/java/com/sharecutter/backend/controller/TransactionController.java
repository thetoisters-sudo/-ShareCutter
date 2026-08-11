package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.dto.common.PagedResponse;
import com.sharecutter.backend.dto.transaction.TransactionCreateRequest;
import com.sharecutter.backend.dto.transaction.TransactionResponse;
import com.sharecutter.backend.dto.transaction.TransactionSearchCriteria;
import com.sharecutter.backend.dto.transaction.TransactionUpdateRequest;
import com.sharecutter.backend.mapper.TransactionMapper;
import com.sharecutter.backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/portfolios/{portfolioId}/transactions"
)
@Tag(
        name = "Transactions",
        description = """
                Manage investment transactions inside a portfolio.

                Transactions may represent purchases, sales, dividends,
                deposits, withdrawals, fees and other supported
                transaction types.
                """
)
@SecurityRequirement(
        name = "bearerAuth"
)
@Validated
public class TransactionController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAXIMUM_PAGE_SIZE = 200;

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    public TransactionController(
            TransactionService transactionService,
            TransactionMapper transactionMapper
    ) {
        this.transactionService =
                transactionService;

        this.transactionMapper =
                transactionMapper;
    }

    @PostMapping
    @Operation(
            summary = "Create a transaction",
            description = """
                    Creates a new transaction inside the specified portfolio.

                    The authenticated user must own the portfolio.
                    When an asset identifier is supplied, the asset must also
                    belong to the same portfolio.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Transaction created successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            TransactionResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Invalid request data or transaction
                            validation failure
                            """
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Portfolio or asset was not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                            Transaction conflicts with the current
                            resource state
                            """
            )
    })
    public ResponseEntity<TransactionResponse> createTransaction(
            @Parameter(
                    hidden = true
            )
            @AuthenticationPrincipal
            UserEntity authenticatedUser,

            @Parameter(
                    name = "portfolioId",
                    description = "Unique identifier of the portfolio",
                    required = true,
                    in = ParameterIn.PATH,
                    example =
                            "1a2b3c4d-5e6f-4789-abcd-0123456789ab"
            )
            @PathVariable
            UUID portfolioId,

            @Parameter(
                    description = "Transaction creation payload",
                    required = true
            )
            @Valid
            @RequestBody
            TransactionCreateRequest request
    ) {
        TransactionEntity createdTransaction =
                transactionService.createTransaction(
                        authenticatedUser.getId(),
                        portfolioId,
                        request.assetId(),
                        request.transactionType(),
                        request.quantity(),
                        request.unitPrice(),
                        request.fee(),
                        request.totalAmount(),
                        request.currency(),
                        request.executedAt(),
                        request.notes()
                );

        TransactionResponse response =
                transactionMapper.toResponse(
                        createdTransaction
                );

        URI location = URI.create(
                "/api/v1/portfolios/"
                        + portfolioId
                        + "/transactions/"
                        + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping
    @Operation(
            summary = "Search portfolio transactions",
            description = """
                    Returns a paginated list of transactions belonging
                    to the specified portfolio.

                    Optional filters may be combined:
                    asset identifier, transaction type, start date
                    and end date.

                    Results are ordered by execution date in descending
                    order, so the newest transactions appear first.

                    When no filters are supplied, all active portfolio
                    transactions are returned.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transactions retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            PagedResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Invalid search parameters, pagination values
                            or date range
                            """
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Portfolio or asset was not found"
            )
    })
    public ResponseEntity<
            PagedResponse<TransactionResponse>
            > getTransactions(
            @Parameter(
                    hidden = true
            )
            @AuthenticationPrincipal
            UserEntity authenticatedUser,

            @Parameter(
                    name = "portfolioId",
                    description = "Unique identifier of the portfolio",
                    required = true,
                    in = ParameterIn.PATH,
                    example =
                            "1a2b3c4d-5e6f-4789-abcd-0123456789ab"
            )
            @PathVariable
            UUID portfolioId,

            @Parameter(
                    name = "assetId",
                    description = """
                            Filter transactions by asset identifier.
                            The asset must belong to the portfolio.
                            """,
                    in = ParameterIn.QUERY,
                    example =
                            "2b3c4d5e-6f70-489a-bcde-1234567890ab"
            )
            @RequestParam(
                    required = false
            )
            UUID assetId,

            @Parameter(
                    name = "type",
                    description = """
                            Filter transactions by transaction type
                            """,
                    in = ParameterIn.QUERY,
                    example = "BUY"
            )
            @RequestParam(
                    name = "type",
                    required = false
            )
            TransactionType transactionType,

            @Parameter(
                    name = "startDate",
                    description = """
                            Include transactions executed on or after
                            this date and time.

                            The value must use ISO-8601 date-time format.
                            """,
                    in = ParameterIn.QUERY,
                    example = "2026-01-01T00:00:00Z"
            )
            @RequestParam(
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            OffsetDateTime startDate,

            @Parameter(
                    name = "endDate",
                    description = """
                            Include transactions executed on or before
                            this date and time.

                            The value must use ISO-8601 date-time format.
                            """,
                    in = ParameterIn.QUERY,
                    example = "2026-12-31T23:59:59Z"
            )
            @RequestParam(
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            OffsetDateTime endDate,

            @Parameter(
                    name = "page",
                    description = """
                            Zero-based page index
                            """,
                    in = ParameterIn.QUERY,
                    example = "0"
            )
            @RequestParam(
                    defaultValue = "0"
            )
            @Min(
                    value = DEFAULT_PAGE,
                    message =
                            "Page index must be zero or greater"
            )
            Integer page,

            @Parameter(
                    name = "size",
                    description = """
                            Number of transactions returned per page
                            """,
                    in = ParameterIn.QUERY,
                    example = "20"
            )
            @RequestParam(
                    defaultValue = "20"
            )
            @Min(
                    value = 1,
                    message =
                            "Page size must be at least 1"
            )
            @Max(
                    value = MAXIMUM_PAGE_SIZE,
                    message =
                            "Page size must not exceed 200"
            )
            Integer size
    ) {
        TransactionSearchCriteria criteria =
                new TransactionSearchCriteria(
                        assetId,
                        transactionType,
                        startDate,
                        endDate,
                        page,
                        size
                );

        Page<TransactionEntity> transactions =
                transactionService
                        .searchTransactionsPage(
                                authenticatedUser.getId(),
                                portfolioId,
                                criteria
                        );

        PagedResponse<TransactionResponse> response =
                PagedResponse.from(
                        transactions,
                        transactionMapper::toResponse
                );

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping("/{transactionId}")
    @Operation(
            summary = "Get a transaction",
            description = """
                    Returns a single transaction by its identifier.

                    The transaction must belong to the specified portfolio,
                    and the authenticated user must own that portfolio.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction retrieved successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            TransactionResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            Portfolio or transaction was not found
                            """
            )
    })
    public ResponseEntity<TransactionResponse> getTransaction(
            @Parameter(
                    hidden = true
            )
            @AuthenticationPrincipal
            UserEntity authenticatedUser,

            @Parameter(
                    name = "portfolioId",
                    description = "Unique identifier of the portfolio",
                    required = true,
                    in = ParameterIn.PATH,
                    example =
                            "1a2b3c4d-5e6f-4789-abcd-0123456789ab"
            )
            @PathVariable
            UUID portfolioId,

            @Parameter(
                    name = "transactionId",
                    description = "Unique identifier of the transaction",
                    required = true,
                    in = ParameterIn.PATH,
                    example =
                            "3c4d5e6f-7081-49ab-cdef-2345678901ab"
            )
            @PathVariable
            UUID transactionId
    ) {
        TransactionEntity transaction =
                transactionService.getTransaction(
                        authenticatedUser.getId(),
                        portfolioId,
                        transactionId
                );

        return ResponseEntity.ok(
                transactionMapper.toResponse(
                        transaction
                )
        );
    }

    @PutMapping("/{transactionId}")
    @Operation(
            summary = "Update a transaction",
            description = """
                    Replaces the editable values of an existing transaction.

                    The authenticated user must own the portfolio.
                    The transaction must belong to that portfolio.
                    When an asset identifier is supplied, the asset must also
                    belong to the same portfolio.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction updated successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            TransactionResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Invalid request data or transaction
                            validation failure
                            """
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            Portfolio, transaction or asset was not found
                            """
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                            Transaction conflicts with the current
                            resource state
                            """
            )
    })
    public ResponseEntity<TransactionResponse> updateTransaction(
            @Parameter(
                    hidden = true
            )
            @AuthenticationPrincipal
            UserEntity authenticatedUser,

            @Parameter(
                    name = "portfolioId",
                    description = "Unique identifier of the portfolio",
                    required = true,
                    in = ParameterIn.PATH,
                    example =
                            "1a2b3c4d-5e6f-4789-abcd-0123456789ab"
            )
            @PathVariable
            UUID portfolioId,

            @Parameter(
                    name = "transactionId",
                    description = "Unique identifier of the transaction",
                    required = true,
                    in = ParameterIn.PATH,
                    example =
                            "3c4d5e6f-7081-49ab-cdef-2345678901ab"
            )
            @PathVariable
            UUID transactionId,

            @Parameter(
                    description = "Transaction update payload",
                    required = true
            )
            @Valid
            @RequestBody
            TransactionUpdateRequest request
    ) {
        TransactionEntity updatedTransaction =
                transactionService.updateTransaction(
                        authenticatedUser.getId(),
                        portfolioId,
                        transactionId,
                        request.assetId(),
                        request.transactionType(),
                        request.quantity(),
                        request.unitPrice(),
                        request.fee(),
                        request.totalAmount(),
                        request.currency(),
                        request.executedAt(),
                        request.notes()
                );

        return ResponseEntity.ok(
                transactionMapper.toResponse(
                        updatedTransaction
                )
        );
    }

    @PostMapping("/rebuild")
    @Operation(
            summary = "Rebuild portfolio state",
            description = """
                    Recalculates all active holdings and the complete
                    portfolio state from the portfolio transaction history.

                    This operation is useful after importing historical
                    transactions, correcting legacy data or recovering
                    derived holding information.

                    The authenticated user must own the portfolio.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = """
                            Portfolio holdings and calculated state
                            rebuilt successfully
                            """
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Portfolio was not found"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = """
                            Portfolio calculation service is unavailable
                            or rebuilding the portfolio state failed
                            """
            )
    })
    public ResponseEntity<Void> rebuildPortfolioState(
            @Parameter(
                    hidden = true
            )
            @AuthenticationPrincipal
            UserEntity authenticatedUser,

            @Parameter(
                    name = "portfolioId",
                    description = "Unique identifier of the portfolio",
                    required = true,
                    in = ParameterIn.PATH,
                    example =
                            "1a2b3c4d-5e6f-4789-abcd-0123456789ab"
            )
            @PathVariable
            UUID portfolioId
    ) {
        transactionService.rebuildPortfolioState(
                authenticatedUser.getId(),
                portfolioId
        );

        return ResponseEntity
                .ok()
                .build();
    }

    @DeleteMapping("/{transactionId}")
    @Operation(
            summary = "Delete a transaction",
            description = """
                    Soft deletes a transaction from the specified portfolio.

                    The authenticated user must own the portfolio,
                    and the transaction must belong to that portfolio.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Transaction deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            Portfolio or transaction was not found
                            """
            )
    })
    public ResponseEntity<Void> deleteTransaction(
            @Parameter(
                    hidden = true
            )
            @AuthenticationPrincipal
            UserEntity authenticatedUser,

            @Parameter(
                    name = "portfolioId",
                    description = "Unique identifier of the portfolio",
                    required = true,
                    in = ParameterIn.PATH,
                    example =
                            "1a2b3c4d-5e6f-4789-abcd-0123456789ab"
            )
            @PathVariable
            UUID portfolioId,

            @Parameter(
                    name = "transactionId",
                    description = "Unique identifier of the transaction",
                    required = true,
                    in = ParameterIn.PATH,
                    example =
                            "3c4d5e6f-7081-49ab-cdef-2345678901ab"
            )
            @PathVariable
            UUID transactionId
    ) {
        transactionService.deleteTransaction(
                authenticatedUser.getId(),
                portfolioId,
                transactionId
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}