#!/usr/bin/env python3
"""Synthetic metadata fault tests, not evidence of host or container execution."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("codec_profile", Path(__file__).with_name("codec-profile-package.py"))
profile = importlib.util.module_from_spec(spec)
spec.loader.exec_module(profile)


class ProfilePackageTests(unittest.TestCase):
    def metadata(self, **changes):
        row = {"Package": profile.PACKAGE, "Version": profile.VERSION, "Architecture": "all",
               "Filename": profile.FILENAME, "Size": str(profile.PACKAGE_BYTES), "SHA256": profile.PACKAGE_SHA}
        row.update(changes)
        return "\n".join(f"{key}: {value}" for key, value in row.items()) + "\n"

    def test_exact_signed_package_record_is_required(self):
        self.assertEqual(profile.validate_metadata(self.metadata())["Version"], profile.VERSION)
        for changes in ({"Version": "newer"}, {"SHA256": "0" * 64}, {"Architecture": "amd64"},
                        {"Size": "1"}, {"Filename": "pool/other.deb"}, {"Package": "other"}):
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                profile.validate_metadata(self.metadata(**changes))

    def test_duplicate_or_oversized_metadata_is_rejected(self):
        for text in (self.metadata() + "Version: other\n", self.metadata() + "\n" + self.metadata(), "x" * 1048577):
            with self.subTest(length=len(text)), self.assertRaises(ValueError):
                profile.validate_metadata(text)

    def test_loaded_child_policy_must_enforce(self):
        profile.validate_loaded("unrelated (enforce)\nbwrap (enforce)\nunpriv_bwrap (enforce)\n")
        for text in ("bwrap (enforce)\n", "bwrap (complain)\nunpriv_bwrap (enforce)\n",
                     "bwrap (enforce)\nunpriv_bwrap (complain)\n",
                     "bwrap (enforce)\nbwrap (enforce)\nunpriv_bwrap (enforce)\n"):
            with self.subTest(text=text), self.assertRaises(ValueError):
                profile.validate_loaded(text)

    def test_modified_package_and_unconfined_stub_cannot_be_installed(self):
        with tempfile.TemporaryDirectory() as directory:
            package = Path(directory) / "package.deb"
            package.write_bytes(b"x" * profile.PACKAGE_BYTES)
            with self.assertRaises(ValueError):
                profile.validate_package(package)
            policy = Path(directory) / "profile"
            policy.write_text("profile bwrap /usr/bin/bwrap flags=(unconfined) { userns, }\n")
            with self.assertRaises(ValueError):
                profile.validate_profile(policy)
            link = Path(directory) / "link"
            link.symlink_to(policy)
            with self.assertRaises(ValueError):
                profile.validate_profile(link)


if __name__ == "__main__":
    unittest.main(verbosity=2)
