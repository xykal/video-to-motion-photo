# Motion Photo Studio design
Warm ivory canvas (#F7F4ED), charcoal typography (#202521), restrained coral action (#D45C4F). Original aperture-and-motion logo in assets/motion-mark.png, generated for this project. Rounded surfaces, compact uppercase step labels, generous spacing. Every screen is a single scrollable column with touch targets at least 46dp. System TextView is used for accessible text rendering; OS default Button styling is not used. No external fonts, icon libraries, image CDNs, or analytics. Dark theme and full screen-reader narration need dedicated device QA.

A dedicated custom-drawn progress meter shows real stage progress; ETA is an approximate extrapolation from elapsed time and stage-weighted progress. No indeterminate spinner pretending to be a completed task.

Trim preview uses Media3 ExoPlayer with an explicit texture-backed surface and an extracted still frame overlay, avoiding the blank paused VideoView surface. Custom aperture-inspired vector icons for select, motion, cut, play, pause, cancel, and confirm live under app/src/main/res/drawable. The trim screen exposes unambiguous cancel and finish actions.


Trim regression fix: the confirm/cancel bar is outside the scrolling content and visible at all times. Dragging a range handle updates the displayed timestamp and a nearby decoded video frame; seeking the player on every drag event was removed to avoid buffering/freezes.
