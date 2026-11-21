"""Ensure public documentation stays consistent with tracked project assets."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
README = ROOT / "README.md"
CATALOG = ROOT / "gradle" / "libs.versions.toml"


class ReadmeIntegrityTest(unittest.TestCase):
    def test_wrapper_is_committed_and_pinned(self):
        for file in (
            "gradlew",
            "gradlew.bat",
            "gradle/wrapper/gradle-wrapper.jar",
            "gradle/wrapper/gradle-wrapper.properties",
        ):
            with self.subTest(file=file):
                self.assertTrue((ROOT / file).is_file(), file)
        prop = (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text(encoding="utf-8")
        self.assertIn("gradle-8.9-bin.zip", prop)

    def test_readme_reports_actual_stack_versions(self):
        catalog = CATALOG.read_text(encoding="utf-8")
        readme = README.read_text(encoding="utf-8")
        for alias, name in (("kotlin", "Kotlin"), ("agp", "Android Gradle Plugin"),
                            ("media3", "Media3"), ("roomRuntime", "Room"),
                            ("retrofit", "Retrofit"), ("okhttp", "OkHttp")):
            match = re.search(r"^" + re.escape(alias) + r' = "([^"]+)"', catalog, flags=re.M)
            self.assertIsNotNone(match, alias)
            self.assertIn("| " + name + " | `" + match.group(1) + "` |", readme)

    def test_screenshots_and_local_links_exist(self):
        readme = README.read_text(encoding="utf-8")
        for i in range(1, 6):
            path = f"fastlane/metadata/android/es-ES/images/phoneScreenshots/{i}.jpg"
            self.assertIn(path, readme)
            self.assertTrue((ROOT / path).is_file(), path)
        for link in re.findall(r"\]\(([^)]+)\)", readme):
            if link.startswith(("http://", "https://", "#", "mailto:")):
                continue
            relative = link.split("#", 1)[0]
            self.assertTrue((ROOT / relative).exists(), f"Broken README link: {link}")

    def test_release_is_not_claimed_store_approved(self):
        readme = README.read_text(encoding="utf-8")
        self.assertIn("Solicitud de inclusión", readme)
        self.assertIn("pendiente de aceptación", readme)


if __name__ == "__main__":
    unittest.main()
