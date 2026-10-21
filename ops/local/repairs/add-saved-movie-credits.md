# Add credits to the two saved movies

This operator repair adds missing credits to the existing Whalefall and Spider-Man: Brand New Day records. It does not create movies, change posters, or alter proposal content, approvals, or operation receipts.

Independent source review accepted this repair. It was applied to the local Catalog database on 21 October 2026: 21 people and 28 missing credit relationships were inserted. The existing movie IDs and poster assets were preserved. No tests or application checks were run.

## Exact changes

- Whalefall, `474c56db-778a-4c0d-90bc-148f423588fe`: 13 relationships covering six actors, one director, two writers, and four producers. Austin Abrams is credited as Jay Gardiner and Josh Brolin as Mitt Gardiner; other character names are omitted.
- Spider-Man: Brand New Day, `88c44f70-9154-46dd-84b3-3e54c1aadfca`: 15 relationships covering eight actors, one director, two writers, and four producers. Character names are omitted.
- Existing active people are reused only by exact name. Multiple active people with the same name abort the whole transaction. Missing people are created. Rachel O'Connor uses a straight apostrophe.
- Existing title/person/role credit relationships remain untouched. Only newly inserted relationships increase the title version and the person's impact version, once per relationship. Person metadata versions remain unchanged. Billing order is omitted.

## Sources and audit

The supplied credits come from [Whalefall's official studio page](https://www.20thcenturystudios.com/movies/whalefall) and [Spider-Man's official Sony page](https://www.sonypictures.com/movies/spidermanbrandnewday). Each inserted person and credit receives matching `operational_event` and `catalog_audit_event` entries with its source URL and the repair identifier. Existing provenance records remain intact.

The actor is `00000000-0000-4000-8000-000000000002`, matching the existing consumed human approvals. The repair correlation is `7dff73d9-406f-4258-aa3d-ce0d352c3a7c`.

## Execution and limits

Run `add-saved-movie-credits.sql` against the established Catalog database with an authorized operator connection. Keep credentials in the existing protected environment or secret mechanism. `ON_ERROR_STOP` and one transaction prevent partial application.

The script requires schema 25, the exact active movie IDs and titles, their existing consumed approvals, and the expected four contribution roles. It locks both movie rows and briefly serializes person/credit writes. Lock acquisition times out after five seconds; statements time out after 30 seconds. A mismatch aborts the transaction and requires inspecting the current records before retrying.

A repeat run inserts no duplicate people or credit relationships and does not increment counters for relationships that already exist. The final notice reports only actual inserted people and credits. The prepared data contains 28 relationships, split 13 and 15 between the two movies.
