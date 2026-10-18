# Use SpoolIn

SpoolIn keeps movie information, the people behind each film, and poster artwork together. Open the local address printed during [setup](local-setup.md#start-and-sign-in) to follow along.

## Contents

- [Browse and search](#browse-and-search)
- [Explore a movie](#explore-a-movie)
- [Find cast and creators](#find-cast-and-creators)
- [Choose the right account](#choose-the-right-account)
- [Create and edit movies](#create-and-edit-movies)
- [Manage people and credits](#manage-people-and-credits)
- [Upload artwork](#upload-artwork)
- [Archive and recover records](#archive-and-recover-records)
- [Understand feedback](#understand-feedback)
- [Use a smaller screen](#use-a-smaller-screen)

The screenshots include fictional walkthrough records alongside the seeded catalog. Seed data does not include poster files; the lighthouse poster was uploaded through the application.

## Browse and search

The **Catalog** page lists movies with poster artwork. Select a movie to open its details. Use search when you know part of a title; filters help narrow the catalog. Wait for results to finish loading before changing your search again. An empty result means the current search has no matches; clear it or broaden the filters to return to browsing.

![Movie catalog showing artwork and browsing controls](assets/spoolin/catalog.png)

## Explore a movie

A movie page shows its synopsis and available release details, with cast and creator credits below. Select a credited person to explore their profile. Missing optional information can appear as unknown; it doesn't mean the movie failed to load.

![Movie details with synopsis, artwork, and credits](assets/spoolin/movie-details.png)

## Find cast and creators

Open **People** to browse or search for someone by name. A person's profile connects their biography with credited movies. Cast and creators share person records; a credit describes what that person did on a particular film. Two people with the same name can have separate records, so check their details before linking a credit.

![People directory with search and person records](assets/spoolin/people.png)

## Choose the right account

| Account | Access |
| --- | --- |
| Visitor | Browse and search public movies, people, and credits |
| Viewer | Browse with a signed-in account |
| Maintainer | Maintain movies, people, credits, artwork, and archived records |

Choose **Sign in** to use the credentials from `.local/accounts.txt`. The local setup creates a Viewer and a Maintainer for trying both experiences. Keep those passwords private. **Register** creates a Viewer account; it doesn't grant Maintainer permissions.

When a session expires, sign in again before continuing an edit. The page explains retained draft recovery when it is available. Don't assume a form is saved just because you can still see its values.

## Create and edit movies

1. Sign in as a Maintainer and open **Movies** in the navigation.
2. Choose **Create movie**. Enter the required title, synopsis, and other fields marked by the form. Add release details and genres where available.
3. Read any field errors and correct the named inputs. The form displays the current limits; don't shorten content based on an old screenshot's limits.
4. Save and wait for confirmation. Open the saved movie to check the displayed information.
5. To change a movie, return to **Movies**, choose **Edit** for that record, edit its values, and save again.

![Movie management view for maintaining catalog entries](assets/spoolin/manage-movies.png)

If someone else saves the record while you're editing, SpoolIn asks you to review the current saved values. Compare them with your retained changes before continuing. If the connection drops during a save, use **Check original result** when offered. Repeatedly submitting an uncertain save can make it harder to tell which operation completed.

The seeded catalog also contains TV titles. Those entries are read-only in the movie management list.

## Manage people and credits

Open **Manage people** in the navigation and choose **Create person** to add a professional name and biography, or **Edit** on an existing record to change it. Save the person before using them in a movie credit.

In a movie's **Cast and creators** section, choose an existing **Person** and **Role**, then select **Add credit**. Cast roles can include **Character names · one per line** when the selected role supports them. Creator roles describe contributions such as directing. Existing rows have **Edit credit** and **Remove credit** controls; editing uses **Save credit**. Review the selected person, role, and character names before saving. Editing a person updates that identity across its credits; it doesn't merge other people with the same name.

## Upload artwork

Open a saved movie's management page and find **Poster artwork**. Choose a PNG, JPEG, or WebP image that meets the limits shown by the application. Use artwork you have permission to use.

The upload first transfers the file, then processes it on the server. A complete transfer percentage doesn't mean the poster is attached yet. Wait for the saved confirmation and check the refreshed artwork. If the page says the artwork attached but details couldn't refresh, retry the details read before uploading again.

Cancel applies to an active operation where the control is available. When the outcome is uncertain, use the offered status check. Unsupported or oversized files should show an explanation so you can choose another file.

![Artwork upload showing the saved confirmation](assets/spoolin/artwork.png)

## Archive and recover records

Archiving removes a movie or person from active catalog browsing while retaining a recovery path. Open the record's management page, choose its archive action, and read the confirmation before proceeding. Inspect affected credits and the impact information shown for the operation.

Use **Archives** in the navigation to review archived records and the actions available for them. Treat permanent deletion as a separate decision: read the impact preview and confirmation rather than assuming it behaves like archiving. **History** in the navigation helps Maintainers inspect recorded catalog changes.

## Understand feedback

| Message or state | What to do |
| --- | --- |
| Loading | Wait for the requested page or operation to finish |
| Field validation error | Correct the named field; the form keeps your other input |
| Saved | Check the resulting record before starting another change |
| Stale record or changed target | Review the newer saved values and reconcile your edit |
| Uncertain result | Check the original operation before submitting again |
| Upload transferring | Keep the page open while bytes are sent |
| Upload processing | Wait for server processing and attachment confirmation |
| Permission or session problem | Sign in with the correct account role |
| Read failure | Retry the read when offered; a read failure isn't proof that a write failed |

Local startup disables external in-chat publication. The catalog and maintenance interface can be explored without connecting a chat provider. External proposal approval and production readiness have separate verification requirements described in the [release records](release/handoff.md).

## Use a smaller screen

Navigation links wrap for narrow screens. Select the same labeled links used on desktop, and scroll to reach form actions and credit sections. Loading, error, and upload feedback remain part of the same journey.

![SpoolIn catalog on a narrow mobile viewport](assets/spoolin/mobile.png)

For the service layout and development references, return to [the root README](../README.md#how-it-works).
