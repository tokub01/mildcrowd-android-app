package de.velwebsolutions.mildcrowd.membership.billing

/**
 * The membership as Google Play subscriptions – the same tiers and prices as the App Store
 * (mildcrowd-ios/Config/Mildcrowd.storekit and `config('mildcrowd.apple.products')` on the server). The product ids drop
 * Apple's bundle prefix because Play allows at most 40 characters; the server maps them through
 * `config('mildcrowd.google_play.products')`. Each id is one subscription in the Play Console with a single auto-renewing base plan:
 * `monat` (P1M) or `jahr` (P1Y, two months free). Play shows its own localised price; the prices below are the ones
 * the products were created with. The server lists them (`google.products` of GET membership), the checkout offers that list.
 *
 * | Product id                   | Tier       | Rhythm | Price           |
 * |------------------------------|------------|--------|-----------------|
 * | `mildcrowd.solidar.monat`    | Solidar    | monat  | 3 € / Monat     |
 * | `mildcrowd.solidar.jahr`     | Solidar    | jahr   | 30 € / Jahr     |
 * | `mildcrowd.mittendrin.monat` | Mittendrin | monat  | 5 € / Monat     |
 * | `mildcrowd.mittendrin.jahr`  | Mittendrin | jahr   | 50 € / Jahr     |
 * | `mildcrowd.maezen.monat`     | Mäzen      | monat  | 12 € / Monat    |
 * | `mildcrowd.maezen.jahr`      | Mäzen      | jahr   | 119,99 € / Jahr |
 */
object PlayProducts {
    /** The server's monthly rhythm key. */
    const val MONTHLY = "monat"

    /** The server's yearly rhythm key. */
    const val YEARLY = "jahr"
}
