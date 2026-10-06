# Seed post images — sourcing list

One image per topic (shared across that topic's 5 tone-variant posts). Save each result using the **exact filename** given — the seeding script will reference `/seed-images/<filename>` directly, no upload step needed since this folder ships with the frontend as a static asset.

Two methods:
- **Gemini / Google Images** — search the prompt as-is, pick a real, high-resolution, editorial/stock-style result (prefer Creative Commons or clearly reusable images where possible — this is a public portfolio project).
- **ChatGPT (image generation)** — paste the prompt into ChatGPT/DALL-E to generate an illustrated/animated-style image. Used only where a real photo would be copyright-risky (game screenshots, movie posters) or where there's no real photo for an abstract concept (AI topics).

Avoid using identifiable photos of real named private/public individuals (politicians, athletes, actors) where their likeness rights are unclear — those topics below are deliberately generic (crowd/stadium/venue shots) or routed to generation instead.

| # | Filename | Method | Prompt |
|---|---|---|---|
| 1 | `01-ai-model-race.jpg` | ChatGPT | "A sleek, abstract illustration of two glowing AI neural networks facing off, blue and violet gradient, minimalist flat tech-art style, no text, no logos, no real brand references" |
| 2 | `02-ai-safety-alarm.jpg` | ChatGPT | "A minimalist illustration of a glowing red warning triangle overlaid on an abstract circuit-board brain pattern, dark background, flat tech-art style, no text" |


| 3 | `03-markets-record-highs.jpg` | Gemini | "stock market trading floor digital ticker board green numbers high resolution editorial photo" |
| 4 | `04-schneider-ptc-ma.jpg` | Gemini | "modern industrial factory handshake business deal corporate photo" |
| 5 | `05-jwst-deep-space.jpg` | Gemini | "James Webb Space Telescope deep field galaxy image NASA public domain" |
| 6 | `06-roman-space-telescope.jpg` | Gemini | "NASA Roman Space Telescope in space illustration or launch photo official NASA image" |
| 7 | `07-orionids-saturn.jpg` | Gemini | "Orionid meteor shower night sky long exposure photo with stars" |
| 8 | `08-nobel-medicine.jpg` | Gemini | "Nobel Prize gold medal close up photo neuroscience lab background" |
| 9 | `09-climate-litigation.jpg` | Gemini | "US Supreme Court building exterior photo daylight" |
| 10 | `10-climate-maternal-health.jpg` | Gemini | "city street heatwave shimmer extreme heat summer photo" |
| 11 | `11-wildlife-esa-policy.jpg` | Gemini | "Florida panther in natural habitat wildlife photography" |
| 12 | `12-ev-milestone.jpg` | Gemini | "electric vehicle charging station modern city street photo" |
| 13 | `13-russia-ukraine-frontline.jpg` | Gemini | "Kyiv Ukraine city skyline daytime photo editorial" |
| 14 | `14-ethiopia-eritrea.jpg` | Gemini | "Ethiopia Eritrea border region landscape photo editorial" |
| 15 | `15-france-protests.jpg` | Gemini | "large street protest crowd Paris France editorial news photo" |




| 16 | `16-horror-box-office.jpg` | ChatGPT | "A moody illustrated movie-theater marquee glowing in the dark with eerie fog and orange lights, horror-movie-season atmosphere, flat poster-art style, no readable text, no real film titles or logos" |






| 17 | `17-sports-ovechkin-mlb.jpg` | Gemini | "ice hockey arena packed stadium lights action photo" |



| 18 | `18-gaming-october-lineup.jpg` | ChatGPT | "A vibrant illustrated flat-lay of a game controller, headset, and glowing screen with neon game-UI elements, colorful digital art style, no logos, no real game titles" |





| 19 | `19-gaming-ai-npc.jpg` | ChatGPT | "An abstract illustration of a humanoid video-game character wireframe with glowing AI neural pathways, digital concept-art style, no text, no logos" |







| 20 | `20-diwali-durga-puja.jpg` | Gemini | "Diwali diya oil lamps lit celebration night photo" |
| 21 | `21-india-voter-protest.jpg` | Gemini | "large peaceful protest crowd India daytime editorial photo" |
| 22 | `22-rajya-sabha-elections.jpg` | Gemini | "Indian Parliament building Sansad Bhavan exterior daylight photo" |
| 23 | `23-tn-wb-elections.jpg` | Gemini | "Indian polling booth ballot box election day photo" |
| 24 | `24-rahul-gandhi-balaghat.jpg` | Gemini | "rural Indian village tribal community daily life photo Madhya Pradesh" |
| 25 | `25-cricket-asian-games.jpg` | Gemini | "cricket stadium celebration crowd floodlights photo" |
| 26 | `26-kabaddi-boxing-asiangames.jpg` | Gemini | "kabaddi match action photo indoor stadium" |




| 27 | `27-bollywood-october-slate.jpg` | ChatGPT | "An illustrated vintage Bollywood cinema hall facade glowing at night with colorful marquee lights, flat poster-art style, no readable text, no real film titles, logos, or actor likenesses" |
| 28 | `28-deepfake-ai-likeness-case.jpg` | ChatGPT | "An abstract illustration of a glitching digital face made of fragmented light particles, representing AI deepfake concern, dark background, no real person depicted, no text" |







| 29 | `29-moneyview-ipo.jpg` | Gemini | "stock exchange digital board green red numbers IPO listing day photo" |
| 30 | `30-simple-energy-ev-funding.jpg` | Gemini | "electric scooter manufacturing factory assembly line photo India" |

## Once you have the images

Drop each file into this folder (`frontend/public/seed-images/`) with the exact filename from the table above. Tell me when they're all in and I'll wire `imageUrl` into the seed-posts dataset and run the seeding script — no further code changes needed on my end, the frontend already serves anything in `public/` at the site root (`/seed-images/01-ai-model-race.jpg`, etc.).

Topics without an image dropped in will simply seed with `imageUrl: null` (the post card already handles that — no image renders, nothing breaks), so you can do this in batches if you don't want to do all 30 at once.
