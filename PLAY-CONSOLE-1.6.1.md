# Ballet Log 1.6.1 — Production 업로드 자료

versionCode 5 / versionName 1.6.1

빌드 산출물:
- Play Store (Production track): `app/build/outputs/bundle/release/app-release.aab`
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.6.1.apk`

---

## Release Notes

### 한국어

```
✨ 1.6.1 업데이트
• 발레 로그 추천하기 — 설정에서 친구에게 앱을 바로 공유할 수 있어요
• 첫 사용자 안내 — 포토 로그 빈 화면에 "+" 버튼을 가리키는 안내 화살표 추가
• 빈 화면 안내 정리 — 각 탭의 안내 문구가 "+" 버튼을 정확히 가리키도록
• 히스토리 — 달력 아래 카드 순서를 포토 로그가 먼저 오도록 변경
```

### English

```
✨ 1.6.1
• Share Ballet Log — recommend the app from Settings
• First-run tutorial — bouncing arrow on empty Photo Log points to "+"
• Cleaner empty-state copy across tabs that points to "+"
• History — Photo Logs come first under the calendar
```

### 日本語

```
✨ 1.6.1
• Ballet Log をシェア — 設定から友達に推薦できます
• 初回チュートリアル — 空のフォトログに「+」を指す矢印を追加
• 各タブの空画面の案内文を整理
• 履歴 — カレンダー下のカード順序でフォトログが先に
```

---

## 1.6.1 변경사항 (개발자 메모)

iOS 1.6.1 기능 동기화 + production 출시 정리.

| 항목 | 변경 |
|---|---|
| `SettingsScreen.kt` | "Share Ballet Log" 섹션 추가. ACTION_SEND with Play Store URL. |
| `PhotoLogScreen.kt` | 첫 사용 튜토리얼 화살표 + bouncing 애니메이션. `TutorialPreferences` 추가. |
| `HistoryScreen.kt` | logsSection 순서: Photo Logs → Classes → Notes |
| `strings.xml` (en/ko/ja) | `tap_record_class`, `tap_new_note` 카피 정정. `settings_share_*`, `photolog_tutorial_tap_here` 신규. |
| 백업 배너 제거 | `BackupBanner.kt`, `BackupBannerPreferences.kt` 삭제 — production 시점에 무의미. |
| `build.gradle.kts` | versionCode 5, versionName 1.6.1, APK 출력 이름 `BalletLog-1.6.1.apk` |

Play Store Share URL: `https://play.google.com/store/apps/details?id=com.hyorita.balletlog`

---

## Play Store (Production) 업로드 절차

1. Play Console → **Production** 트랙 → **Create new release**
2. **App bundle** 에 `app-release.aab` 업로드 (versionCode 5 자동 인식)
3. **Release notes** 에 위 한/영/일 텍스트 붙여넣기
4. **Save → Review release → Roll out to production**
5. 단계 출시 비율 결정 (예: 20% → 50% → 100%) 또는 즉시 100%

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → **새 버전 등록**
2. APK 업로드: `BalletLog-1.6.1.apk`
3. 위의 한국어 release notes 붙여넣기
4. 심사 제출

---

## 출시 후 메모

- 1.6 사이드로드 사용자 4명: 원스토어에서 1.6.1 받으면 백업 메시지 사라지고
  Share 섹션 추가됨. 이미 1.6 단계에서 백업 안내 받은 상태라 자연스럽게
  업데이트만 받으면 끝.
- Play Store production 첫 사용자: 빈 상태 + 튜토리얼 화살표로 "+" 버튼
  안내. 첫 PhotoLog 작성 시 자동으로 사라짐.
