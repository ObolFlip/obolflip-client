package org.ergoplatform.obolflip

import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.NetworkType
import org.ergoplatform.appkit.Parameters

object Constants {
    const val isMainNet = true

    const val ticketsToSell = 100L
    const val ticketPrice = Parameters.OneErg

    val coolDownMinutes = if (isMainNet) 25 else 2
    val maxRoundTimeHours = if (isMainNet) 24 else 2

    const val headTokenNamePrefix = "ObolFlip Heads Token #"
    const val tailTokenNamePrefix = "ObolFlip Tails Token #"

    const val flipStateActive = 1L
    const val flipStateInit = 0L

    val networkType by lazy { if (isMainNet) NetworkType.MAINNET else NetworkType.TESTNET }

    val obolFlipAddrString = if (isMainNet)
        "3i5auA2BGj7SCGSiz7omHfQXxU9Tqm6nHcHUgpRasm2nCvcNCJTsFMA39jNp93zubwbYuUXCoJhm9AemFC4TKzD2mhF8Ancc1F6xoKasRgBYneNeU6NoC3RP7S4eB3STThcFaDSjjL3NmEhasxtpUrHRZxUaYo8Qs6nkwbd7LfBKTn3HyiXNDcWZfMzmx8sGN5kaV3CepaVfa7AeBfDwBDwswwJkE8g9NdbYm1S7BYUMNRGAUxd6ZMBJC7Lr5NjdkM2wf89DXMPGFXvU2VYRRyjbMDQEv9EzmrwugomusdN64qojH5ek6RBTiKkKSBgp4xRFbUNfcKLPWqfHgNnm51QNXvdEXF5c55R1uE1X34RBDpjm8nJS4LvEsaSYxP2AXy8SfxrhimTQzV1EF9vHtWWCiKtSCcEZ6wrZajdjvEtWLrzGEc4HXMXVXLdMNSEykZX5eanJdLVPc3rreRPgKtcMZpoiU9d6Un2WERT2yFqCWhTiQ2eM4Ug6eRpaz6tqK9gkfeNKVstCMXuB6HMv6KbNXnPAwMrZSVWet8qVG21ux46wUddnv1ARBv5HYfpUBiz2tTzChuBhBw1p4LjF5tHuJADPhyNma3k1HgAHHawVnhyLtmnEcmDenhZKnXRWe39ysJmJ4TrwtZHpaSncx8cHpm3DMobtSaG4Upv7Y6Khr9J1EEU2RBWgRsAbkuGJM82GTdB3CQ5tMtttYcAkRZqtquZMUF8WaByv3p8xfMAKu2mkcQUduYXffCYLC8F2KmK3cySU6axY3PuDvJZ3DhwSRk9YuH9rJJAdJ4vFaBRk8VXsB9ViDHqw4kyxXzczG4LBzhsbSdsRwxVE9rQk2HJjnQTwwbRPfbTEfXEbPB2dT6rTvzU7UA6to77BGxobVPYTW7pRfGYLTUraRmUhiBYxbRSfPviQAeAZAJj2UeX8T7S7aJ7m91Ek6Zdfgccko1gGqEKKBpeQ49WikbbiKcTB4zisstKefQeWkMsw2bBYq6zyQFm4j4wEXuFkWVjxP4R468KbWZEuTVQZna9x4RiCgfqH2AGdennD4BvEbvZ4pTi6p4WGDToSw25CJDqrARQGonmLrrynW2zUvaivZys3bPpKaXVsrz3HuJNhbWbDLFvg7KwycxwBmjTi9kK8DmBrJ6dEvjDcJkrCb4zxEoi5HuQ5D2gXVE4rNh6GiNmqNG6PhGz36jks6fHyihr4G9jVTwiqTyZxtNdPSyQAxJXRzDHydWP6B9TARpT7kNf9FcC2iK3UYRQHPNwvhe8z6t8iFtwZvLLdQ9AuaobB26Uu4skcNt4RpPNdrqzF8ajH7X7G9ktZapFRDgVUsosMk57k3ggzJoCr4gmANL73m8exAWnys5VuiQYGPW4DbWZKEVdqYsJZWeGfBeEmWzhpv2ELVfcfxJwHwT7c2tmbvUDR36Nc6NgZKeoRoLG64Jh4t2NAeTBWbeH53yN2XaHJ4u8m9XYUrn9QfDASFnw42FbcKgVDqbseBmjDtUSwMiKLKCFFoNcJ84HNFNnQY6hCbb8ootq7ASRv7gaALTxv9bUQBH6oYtdmxsHuDfHw7"
    else
        "4pRoz17P93Cez1ML8joGNzQCo4qcSKWiP6PxCpRfV898mfAkYjsHWnF8QTWrqRL7KJwYvFoGdA2HYxu1qcQp44c3bqHLCy9Gda1t1KiePT8cG7Uz4saw4mQJhowGfJsBXYJCBryaNKwA9byRByWngSW5ze8dJ4jTP13aCsNbmxukNLsRk89GVV8S6jD2gfdAUSzeffAubsbanu5gXhEQjEm5M9t3Tcm4x6TdywdKxePN5c16CPBDQzXUruQw8fncMxweALoietZ9v8pxS1htv9obGD3vgteXnPm1RmPKmTRnXhxTGSAFNN9BJicsd6W2mZBr2GdE9Hc76tMxhaoe8nFrQzKM1Q6JAM2bTQp9WwAaYfdHtBe2J6mDLBzLEnK9ym8Hv9vYjPhrbgLnT3MYdp8AqHnMnuwX8ADrRra9z2TR5zeLopM26ACdA7vFMxcVMNh1UG54XAWkrk38CALGf86vPmqJFctiPwhtn8UeXq6UdnKh7YTdWHWxzXXHdGkYmoT13EevfN1xY7k9tiET6Us5vr3iTENdXXSJqPHxnEkam97N3LejnVKtMggVs4Q8Xveb57JT5G9AKQsiYR5uGVSgv2NzfHX7wFJQZqS4JGTQWrGZtD5EQZk8GymvGGzXha32aCbJVsSx9JRJ9NMs3aKmenBn6q8VGYVsHrAv9aHFwnUHgtCAhD1MaEbHSEBXc4jKxPTYtiJiF24Ui19jwmsRk63gSX9Dig6gkseKsCPUzqK6NUjziAwYjvhAejAt3cKLDis1yD6jtojr1a51JocwdrMFv3q378h1Pp4Dn8cy4EQWM6dPAnzAExpbRVeUzG8pGHEMjWXV2Tev4u99VBdEtA2ia7eRZKcLgz15z5RyzytNptk8X2taPV9rGFBAWbpqiA4BntVyDtXs91Bn7N1xZ5n5Z5WM6KXYuR4axVa7Js29grRbxt9DeDNWmz8VGUzh8hMYXwrE4Kcm7nfywCzMyz1qfyHt6exPJ9FbiPXuzDukQeFGPjw54xzc1b4uxxMUif13zPeBWiUtfPcUWAmUPSwVn31eagDBMyHCz1gc7MwwvLVb4G6bDmvcUt7DSAmSQ3uFBc19tr71TdR1MxBVfFpHQWkH944gJDtdssadkzShDABrAJYr5nyx5j8LR3GaWELmyHpShiSsgEah4nvrKB22jRVf6vRdSH5FFwCsse8wcg8o5j2TxZ9sWLzTraXUZyYQd7dwGuqhY2EfDhZprh77WfG4gFtcQi1nQBdwGQYQ2JpdMP59Y7ihMTT6ZwGj8s3cN7RSUuzwXtawZfQhu5haLQVZ8nLNxUb7HHimb34MpD7Ba1fSSkYb4uLPaxAEyQPtMYuWGGUdAfPWhDBknVqYrxvqXSTrBgbyXkrzvJosRWBMsh8vRX7ZHxXtV11XeFzqGLBDWpWX5qRhuLoXyL2hSasuDKmC9dpUcTbRFce1QgphCiRjhXSoLep79j5zkEAmo1FSUxPkgiSA953AuvymLNFeKTHVMLnCkS4ocHrHcMi9oJKSYtTNhLwun1EfXvdE8SC3fNdRAHB3bbwBAQ2TSAoekWfgcLeS"
    val obolFlipAddress: Address by lazy {
        Address.create(obolFlipAddrString)
    }

    val coolDownAddrString = if (isMainNet)
        "4civ6V8KAXDy7NHZswCT3hgkAeJXV1ntuJb369FHc3gSmv8ycauaeK6nYyMwvARanfuzrzj3t6YckioL81zVsxchENALfyuk2CrzEx2XY197Yk885uooiHzpzRDogdHrCP6usjpq6jsdggqMLFEqQLAar4nYgiGL96BS27vDSGbgR8sHjeovmpUpfwLtpE6r2ArBqW2ZLnkSXFLoeX1fk5L694GQQVTs7LHbPQPBTTM98VUTXD53AzLq7ePJPVREzdtPiSGC7whUaEcppanzCKWmBJjF5wwt2fv8hyeFPnYHg4Gk2dtbKpMm5Zuz7WkskSwoNkuKbaoYAsmTDFq5ZthuTcSqB22wVkVKaJRiJRhQJw9FyBFx8ZBhtKzBu3G2o32KqpQmLyuTd"
    else
        "PWKnFKMNDDQyLyQvuFKEsdaCu8utgC1s5nvwLYv9H3u1iZtB76bheJcCEXJwjNEPKNNhjWjwr7sHqksMkQdhvYDrfVxnhrYjKkkqYH5gioSWW9hFFJPLJ8h3i1ezS1Ubvtf3MK2hgL1TWGNiwvDYvZtPtRt7VyVw2G8eSWq2wF2HXJhYgrzqwxseSjZ15PhrAtePsok7hJTtyZtbN4rKmbcrzyXi2DZ9jaLHdKyNa9phwcS2q96soJxqr3QAenEquhMuVKXfyiMDLHFrzzDv5xMegU4NvR71uyBLWe2FnUfiRnDgJovBTAhGrcip2LBJGHHajiGxEDQ8YUEGfnkvJtHLY8XPnCj1Cvqi3sdT7qEj7xLFJoNWe98UoG4tW4nitkqyY1daath8w"
    val coolDownAddress: Address by lazy { Address.create(coolDownAddrString) }

    val payoutAddrString = if (isMainNet)
        "7gadWEfvYkfP1xEZRPkvyyM3EM827HHMzccC1TeVsfLqjBjCgZ3cFg4M13Vyupsb1SMoHD2cmsrBbS1bnpGUxvV1GgHooQJtfTSZvG1T9qSv35iJkkb8KRSU5opt9mYEekrec91QiNZqrb7N7N3W4qKH9tBSthEDmmPokZNg2MjhHQcxQuBFwBtAkfjobW5T"
    else
        "ibSsgxDfzUDu8TMQm7tzkkKV6XGRt2QySB6zViS6eGNqtWguwVYjAgc6i9vbt2VgR4GqsGSAdhumyvn4adr8aRSnk3qWxN8Lv1BocQLTnVsHcsA1vDdvN1bWpqdv3zApaX8hSmHAxHjcYNuUn48QVhafq6ybqiL52fiDpWKuzLM95FeFvZ3cJPdVkis55UZo"
    val payoutAddress: Address by lazy { Address.create(payoutAddrString) }

    val oracleNftId =
        if (isMainNet) "011d3364de07e5a26f0c4eef0852cddb387039a921b7154ef3cab22c6eda887f"
        else "20292e74624f577f8b6563f68e7d4aa7bc6555732e6923ff69b5bcddd54f947a"
    val markerNftId =
        if (isMainNet) "c218a22c60e9c6544782eecca9080a6cdb45684d82b27978c344327c2a849173"
        else "6d75a19985786dc2892d072edce914077854d1a8ded2d48ae8d689a6b307c02e"

    val softwareLicenseFee by lazy {
        Address.create(
            if (isMainNet) "9fWSMYgZT6gZKdXtVYT9khaXMj6nRYyHQj3FuzWQjSapmmwPbGK"
            else "3Wwxnaem5ojTfp91qfLw3Y4Sr7ZWVcLPvYSzTsZ4LKGcoxujbxd3"
        )
    }
}