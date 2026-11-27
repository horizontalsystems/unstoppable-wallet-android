# UI refactoring

The interface is being rebuilt on one set of tokens and components that
matches the Unstoppable Design System file in Figma
([#9887](https://github.com/horizontalsystems/unstoppable-wallet-android/issues/9887)).
The rules every UI change follows are in the "UI work" section of
`AGENTS.md`. This document holds the rules of that work that need more room.

## Icons

- Icons live in `designkit` (`designkit/src/main/res/drawable`). Look there
  before adding a drawable, and do not add a second drawing of an icon it
  already has.
- Only icons confirmed in Figma go there: the components of the "Icons" and
  "Icons Filled" sections of the design system file.
- A file is named as its component in lower case with the size, so
  `Arrow_M_Down` is `arrow_m_down_24` and `Shield_Check_Filled` is
  `shield_check_filled_24`. They are all 24 dp; a smaller use sets the size
  where the icon is drawn.
- An icon Figma lacks is not added to `designkit`. Ask the designers for it;
  until it is in Figma it stays outside, in `walletkit`.
- Logos, coin and chain art, flags, launcher art and illustrations are not
  icons and stay outside `designkit`.

### Icons still outside `designkit`

These have no icon in Figma, so they stay in `walletkit` or
`components/icons`. Do not replace one with a look-alike from `designkit`;
it moves when Figma has it.

- Drawings the designers decided to keep (November 2026): `flask_24`,
  `ic_app_status`, `ic_blocks_24`, `icon_24_circle_coin`, `ic_clipboard_20`,
  `ic_passcode`, `ic_score_excellent_24`, `ic_score_good_24`,
  `ic_score_fair_24`, `ic_score_poor_24`, `ic_tor_connection_24`,
  `prem_portfolio_24`, `send_24`, `icon_24_markets`, `ic_settings_jacob`,
  `ic_error_48`.
- Control states that are not one tinted shape: `checkbox_active_24`,
  `checkbox_inactive_24`, `radio_on_24`, `radio_off_24`,
  `selector_unchecked_20`. The selected selector is the `designkit` icon
  `done_e_filled_24`, drawn through `FilledCheckMark`.
- Gradient fills, which a tint cannot draw: `defense_gradient_filled_24`,
  `ic_defense_shield_20`, `premium_filled_24_gradient`.
- Logos of other services: `ic_github_20`, `ic_reddit_20`,
  `ic_ton_connect_24`, `ic_wallet_connect_24`, `icon_nym_vpn_24`.
- Placeholders and a helper: `ic_circle_placeholder_20`,
  `icon_24_nft_placeholder`, and `warning_octagon_24`, the octagon drawn
  behind `warning_filled_24` on a coin icon.
