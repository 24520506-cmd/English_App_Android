import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DESIGN = ROOT / "star_journey_unified_auth_full_import/STAR_JOURNEY_UNIFIED_AUTH/design.json"
BASELINE = ROOT / "docs/figma-baseline.json"


class ImportContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.design = json.loads(DESIGN.read_text(encoding="utf-8"))
        cls.screens = {screen["id"]: screen for screen in cls.design["screens"]}

    def buttons(self, screen_id):
        return [block for block in self.screens[screen_id]["blocks"] if block["type"] == "button"]

    def test_primary_screens_do_not_offer_demo_response_buttons(self):
        forbidden = {
            "register_validation", "onboarding_topics_error", "onboarding_name_error",
            "catalog_empty", "lesson_locked", "lesson_continue", "vocab_audio_error",
            "listening_loading", "listening_audio_error", "listening_not_finished",
            "listening_missing", "listening_submitting", "writing_validation",
            "writing_missing_target", "writing_dictionary_warning", "writing_save_failed",
            "writing_feedback_pending", "writing_feedback_deferred", "writing_feedback_error",
            "writing_revision_success", "writing_revision_blocked", "lesson_points_capped",
            "review_today", "review_upcoming", "review_expired", "review_revoked",
            "progress_empty", "ranking_no_listening", "ranking_report_success",
            "profile_name_cooldown", "preferences_saved", "notification_denied",
            "admin_publish_error",
        }
        primary = [
            "register", "onboarding_topics", "onboarding_name", "catalog",
            "lesson_detail", "vocab_1", "listening", "writing",
            "lesson_complete", "writing_feedback", "writing_revision", "review",
            "progress", "ranking", "report_name", "profile_edit", "preferences",
            "notification_settings", "admin_publish_check",
        ]
        offenders = [
            (screen_id, button["title"], button.get("route"))
            for screen_id in primary
            for button in self.buttons(screen_id)
            if button.get("route") in forbidden
        ]
        self.assertEqual([], offenders)
        self.assertTrue(forbidden.issubset(self.screens), "Response variants must remain in the design package")

    def test_welcome_and_recovery_offer_only_real_user_actions(self):
        self.assertEqual(
            [("Đăng nhập", "home"), ("Tạo tài khoản", "register"), ("Quên mật khẩu", "forgot_password")],
            [(b["title"], b["route"]) for b in self.buttons("welcome")],
        )
        self.assertEqual(
            [("Gửi mã", "reset_sent")],
            [(b["title"], b["route"]) for b in self.buttons("forgot_password")],
        )
        self.assertEqual(
            [("Xác minh mã", "reset_password")],
            [(b["title"], b["route"]) for b in self.buttons("reset_sent")],
        )
        self.assertEqual(
            [("Đặt lại mật khẩu", "reset_complete")],
            [(b["title"], b["route"]) for b in self.buttons("reset_password")],
        )
        self.assertEqual(
            [("Đăng nhập lại", "welcome")],
            [(b["title"], b["route"]) for b in self.buttons("reset_complete")],
        )

    def test_home_learning_cards_have_real_destinations(self):
        journeys = [b for b in self.screens["home"]["blocks"] if b["type"] == "journey"]
        self.assertEqual(["lesson_detail", "topic_detail"], [b.get("route") for b in journeys])

    def test_vocabulary_position_is_clear_and_matches_sample_lesson(self):
        for position in range(1, 8):
            screen = self.screens[f"vocab_{position}"]
            self.assertEqual("Học từ vựng", screen["title"])
            indicator = [block for block in screen["blocks"] if block["type"] == "vocab_progress"]
            self.assertEqual([{"type": "vocab_progress", "position": position, "total": 7}], indicator)
            self.assertFalse(any(block["type"] == "progress" for block in screen["blocks"]))

    def test_learner_default_does_not_show_admin_or_pending_feedback_action(self):
        self.assertNotIn("Quản trị (admin only)", [b["title"] for b in self.buttons("profile")])
        feedback = next(b for b in self.screens["lesson_complete"]["blocks"] if b.get("title") == "Nhận xét AI")
        self.assertIn("Đang xử lý", feedback["body"])
        self.assertNotIn("Xem nhận xét bài viết", [b["title"] for b in self.buttons("lesson_complete")])

    def test_screen_count_and_routes_match_package(self):
        self.assertEqual(len(self.screens), self.design["meta"]["screenCount"])
        missing = [
            (screen["id"], block["route"])
            for screen in self.design["screens"]
            for block in screen.get("blocks", [])
            if block.get("route") and block["route"] not in self.screens
        ]
        self.assertEqual([], missing)

    def test_import_preserves_live_screen_inventory_and_repaired_destinations(self):
        baseline = json.loads(BASELINE.read_text(encoding="utf-8"))
        live_ids = {
            screen["name"].removeprefix("STATE · ").split(" · ")[0]
            for screen in baseline["inventory"]["screens"]
        }
        self.assertEqual(live_ids, set(self.screens))
        ranking = self.screens["ranking"]
        selector = next(block for block in ranking["blocks"] if block["type"] == "segment")
        self.assertEqual(
            ["ranking", "ranking_month", "ranking_alltime"],
            [option["route"] for option in selector["options"]],
        )
        self.assertIn("report_name", [block.get("route") for block in ranking["blocks"]])
        self.assertIn(
            "admin_revoke",
            [block.get("route") for block in self.screens["admin_published"]["blocks"]],
        )


if __name__ == "__main__":
    unittest.main()
