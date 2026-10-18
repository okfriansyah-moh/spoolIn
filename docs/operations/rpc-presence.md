# RPC field presence correction

The existing field names and numbers are preserved: `VersionedId.expected_version`3, `CreditWrite.expected_version`7 and `MovieValues.synopsis_language`8 now carry explicit proto3 optional presence. No stored record, receipt or historical request hash is rewritten by this wire change.

A missing required version must fail at the authoritative owner; an explicitly encoded0 remains a valid candidate version. Omitted synopsis language means `und`, while an explicitly supplied value is validated. Receivers can now distinguish those inputs. Native protobuf round-trip and owner rejection tests are part of the remote suite; source changes alone do not establish their execution.

Regenerate and roll out all selected clients with the receiver. Older proto3 senders omit a scalar0 even when their caller supplied it; a new strict receiver rejects that absent version. This is an intentional correction requiring coordinated client deployment, not a claim of transparent compatibility for old zero-version requests. Existing nonzero field encodings remain readable. Unknown-field handling and RPC method names are unchanged.
