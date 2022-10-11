package org.ergoplatform.obolflip.mosaik

import org.ergoplatform.mosaik.*
import org.ergoplatform.mosaik.model.MosaikApp
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.mosaik.model.ui.ForegroundColor
import org.ergoplatform.mosaik.model.ui.ViewGroup
import org.ergoplatform.mosaik.model.ui.layout.HAlignment
import org.ergoplatform.mosaik.model.ui.layout.Padding
import org.ergoplatform.mosaik.model.ui.layout.VAlignment
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.obolflip.Config
import org.ergoplatform.obolflip.formatErgAmount
import org.ergoplatform.obolflip.handler.PayoutBox
import org.ergoplatform.obolflip.millisecondsToRelativeTime
import org.ergoplatform.obolflip.service.ObolFlipService
import org.ergoplatform.obolflip.service.PayoutService
import org.springframework.core.io.ClassPathResource
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import java.lang.Integer.min

@RestController
@CrossOrigin
class MosaikAppController(
    private val obolFlipService: ObolFlipService,
    private val payoutService: PayoutService,
    private val config: Config,
) {
    private val obolFlipVersion = 1

    @GetMapping("/")
    fun mosaikWebApp(): ModelAndView {
        return ModelAndView("index.html")
    }

    @GetMapping("/flipoverview")
    fun getOverviewApp(): MosaikApp {
        return mosaikApp(
            "ObolFlip",
            obolFlipVersion,
            "Bet on heads or tails",
            targetCanvasDimension = MosaikManifest.CanvasDimension.COMPACT_WIDTH,
            cacheLifetime = 120
        ) {
            column(childAlignment = HAlignment.JUSTIFY, spacing = Padding.DEFAULT) {
                val currentFlip = obolFlipService.getCurrentObolFlipState()

                card {
                    layout(HAlignment.JUSTIFY, VAlignment.CENTER) {
                        column(Padding.DEFAULT) {
                            label("Current flip", LabelStyle.HEADLINE2)
                            label("Round #${currentFlip.roundNumber}", LabelStyle.BODY1BOLD)
                            label(
                                (
                                        if (currentFlip.isInitializing)
                                            "Initializing..."
                                        else if (currentFlip.endTimeMs < System.currentTimeMillis() && (currentFlip.headsSold == 0L || currentFlip.tailsSold == 0L))
                                            "Waiting for bets"
                                        else if (currentFlip.endTimeMs < System.currentTimeMillis())
                                            "Closing soon"
                                        else
                                            "Closing in ${millisecondsToRelativeTime(currentFlip.endTimeMs)} or when ${currentFlip.ticketsToSell} bets made."
                                        ),
                                textAlignment = HAlignment.CENTER
                            )

                            hr(Padding.DEFAULT)

                            label("Bet for ${formatErgAmount(currentFlip.ticketPrice)} ERG")

                            if (!currentFlip.isInitializing) {
                                label(
                                    "Pick heads or tails to buy a participation ticket.",
                                    LabelStyle.BODY2,
                                    textAlignment = HAlignment.CENTER
                                )
                                row(
                                    Padding.HALF_DEFAULT,
                                    packed = true,
                                    spacing = Padding.DEFAULT
                                ) {
                                    button("Pick heads") {
                                        onClickAction(invokeErgoPay(config.ergopayPrefix + "/purchaseTicket/0/#P2PK_ADDRESS#"))
                                    }
                                    button("Pick tails") {
                                        onClickAction(invokeErgoPay(config.ergopayPrefix + "/purchaseTicket/1/#P2PK_ADDRESS#"))
                                    }
                                }
                                label(
                                    "Current bets: ${currentFlip.headsSold} heads, ${currentFlip.tailsSold} tails.",
                                    textAlignment = HAlignment.CENTER
                                )
                                hr(Padding.HALF_DEFAULT)
                                label(
                                    "All winners will get their share of the amount that was bet in total, 3% flip management fee withdrawn.",
                                    LabelStyle.BODY2,
                                    HAlignment.CENTER
                                )
                            }
                        }
                    }
                }

                obolFlipService.getCoolDownState()?.let { coolDownState ->
                    card {
                        layout(HAlignment.JUSTIFY, VAlignment.CENTER) {
                            column(Padding.DEFAULT) {
                                label("Flipping", LabelStyle.HEADLINE2)
                                label("Round #${coolDownState.roundNumber}", LabelStyle.BODY1BOLD)
                                label(
                                    if (System.currentTimeMillis() < coolDownState.transitioningTime)
                                        "Flip done in ${millisecondsToRelativeTime(coolDownState.transitioningTime)}"
                                    else "Flip done in a few moments"
                                )
                                hr(verticalPadding = Padding.DEFAULT)
                                label(
                                    "Bets: ${coolDownState.headsSold} heads, ${coolDownState.tailsSold} tails.",
                                    textAlignment = HAlignment.CENTER
                                )

                            }
                        }
                    }
                } ?: payoutService.getPayoutBoxesList().firstOrNull()?.let { payoutBox ->
                    payoutBoxCard(payoutBox, this@mosaikApp)
                }

                row {
                    layout(weight = 1) {
                        label("Reload", LabelStyle.BODY1LINK, textColor = ForegroundColor.PRIMARY) {
                            onClickAction(reloadApp())
                        }
                        label(
                            "Past flips",
                            LabelStyle.BODY1LINK,
                            textColor = ForegroundColor.PRIMARY,
                            textAlignment = HAlignment.END
                        ) {
                            onClickAction(navigateToApp(config.publicurl.trimEnd('/') + "/pastFlips"))
                        }
                    }
                }
            }
        }
    }

    @GetMapping("/pastFlips")
    fun getPastFlipsApp(@RequestParam(defaultValue = "0") page: Int): MosaikApp {
        val pageSize = 4
        val payoutBoxesList = payoutService.getPayoutBoxesList()
        val subList = payoutBoxesList.subList(
            page * pageSize,
            min(payoutBoxesList.size, (page + 1) * pageSize)
        )
        return mosaikApp(
            "ObolFlip Past Flips",
            obolFlipVersion,
            "Claim wins",
            targetCanvasDimension = MosaikManifest.CanvasDimension.COMPACT_WIDTH,
        ) {
            column(spacing = Padding.DEFAULT) {
                subList.forEach { payoutBox ->
                    payoutBoxCard(payoutBox, this@mosaikApp)
                }

                if (payoutBoxesList.size > pageSize * (page + 1)) {
                    label(
                        "Older flips",
                        LabelStyle.BODY1LINK,
                        textColor = ForegroundColor.PRIMARY
                    ) {
                        onClickAction(navigateToApp(config.publicurl.trimEnd('/') + "/pastFlips?page=${page + 1}"))
                    }
                }
            }
        }
    }

    private fun ViewGroup.payoutBoxCard(payoutBox: PayoutBox, app: MosaikApp) {
        card {
            layout(HAlignment.JUSTIFY, VAlignment.CENTER) {
                column(Padding.DEFAULT) {
                    label("Last flip", LabelStyle.HEADLINE2)
                    label("Round #${payoutBox.roundNumber}", LabelStyle.BODY1BOLD)

                    hr(Padding.DEFAULT)

                    label("Winner: " + if (payoutBox.headsWon) "heads" else "tails")
                    label("Winning ${formatErgAmount(payoutBox.receipt)} ERG each")

                    column(Padding.HALF_DEFAULT) {
                        label(
                            "If you've bet successful, you can claim your win by submitting your participation token. 1% UI fee withdrawn.",
                            LabelStyle.BODY2,
                            textAlignment = HAlignment.CENTER
                        )
                        button("Claim win") {
                            onClickAction(app.invokeErgoPay(config.ergopayPrefix + "/redeem/${payoutBox.roundNumber}/#P2PK_ADDRESS#"))
                        }
                    }
                }
            }
        }
    }

    @GetMapping("/mosaikconfig.json")
    fun getConfig(): String {
        val classPathResource = ClassPathResource("mosaikconfig.json")

        return String(classPathResource.inputStream.readAllBytes()).replace(
            "{publicurl}",
            config.publicurl
        )
    }
}