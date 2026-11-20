"""Unit tests for distribution audit without Android SDK or network."""
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))

from audit_distribution import check_dependency_graph, check_project


class DistributionAuditTest(unittest.TestCase):
    def test_rejects_play_services(self):
        graph = "+--- com.google.android.gms:play-services-location:21.3.0\n"
        self.assertTrue(check_dependency_graph(graph))

    def test_rejects_firebase(self):
        graph = "\\--- com.google.firebase:firebase-analytics:22.1.0\n"
        self.assertTrue(check_dependency_graph(graph))

    def test_accepts_open_libraries(self):
        graph = "+--- androidx.media3:media3-exoplayer:1.3.1\n"
        self.assertEqual([], check_dependency_graph(graph))

    def test_current_project_network_policy(self):
        self.assertEqual([], check_project(ROOT))


if __name__ == "__main__":
    unittest.main()
