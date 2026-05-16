# Ballet Log 1.6.1 — Production 업로드 자료

versionCode 6 / versionName 1.6.1

> versionCode bump 이력: 5 → 6 (Play Store 정책 대응 — `READ_MEDIA_IMAGES`
> 권한 제거. PhotoPicker 사용으로 권한 불필요).

빌드 산출물:
- Play Store (Production track): `app/build/outputs/bundle/release/app-release.aab`
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.6.1.apk`

---

## Play Store — 정식 첫 출시용 Release Notes

Play Store Production 트랙 첫 등록이라 "1.6.1 변경점" 보다는 앱 자체를
처음 만나는 사용자에게 무엇을 할 수 있는지 알려주는 톤.

### 한국어

```
발레 클래스를 기록하는 가장 간단한 다이어리, 발레 로그입니다.

• 포토 로그 — 그날의 순간을 캡션, 운동 정보와 함께 남기기
• 클래스 — 바·센터의 동작과 콤비네이션, 사진, 음악 기록
• 메모 — 사진 첨부, 영상 링크, 클래스에 연결
• 히스토리 — 한눈에 보는 월별 캘린더
• Health Connect — 운동 시간·칼로리·심박수 자동 가져오기
• 백업 & 복원 — 모든 데이터를 한 파일로 보관

발레 일지를 매일 가볍게.
```

### English

```
Ballet Log — a simple journal for your ballet classes.

• Photo Log: capture moments with captions and workout overlays
• Class: barre and center steps, combinations, photos, music
• Notes: photo attachments, link to a class, rich link cards
• History: monthly calendar with photo logs, classes and notes
• Health Connect: automatic workout duration, calories, heart rate
• Backup & Restore: export your whole library to one file

A lightweight ballet diary, every day.
```

### 日本語

```
バレエレッスン専用のシンプルな日記アプリ「バレエログ」。

• フォトログ:キャプションやワークアウト情報付きで瞬間を残す
• クラス:バー・センターのステップとコンビネーション、写真、音楽
• メモ:写真添付、レッスンへのリンク、リッチリンクカード
• 履歴:月別カレンダーで一覧
• Health Connect 連携:時間・カロリー・心拍を自動取得
• バックアップ&復元:全データを1ファイルに書き出し

毎日気軽にバレエ日記を。
```

> **Store listing → Full description** 도 같이 채우는 게 좋습니다.
> 위 한국어 텍스트를 살짝 풀어서 앱 페이지 본문에도 사용 가능.

---

## 원스토어 1.6.1 Release Notes (이미 업로드 완료)

기존 1.6 사용자 4명을 위한 업데이트 노트. iOS 1.6.1 동기화 톤.

### 한국어

```
✨ 1.6.1 업데이트
• 발레 로그 추천하기 — 설정에서 친구에게 앱을 바로 공유할 수 있어요
• 첫 사용자 안내 — 포토 로그 빈 화면에 "+" 버튼을 가리키는 안내 화살표 추가
• 빈 화면 안내 정리 — 각 탭의 안내 문구가 "+" 버튼을 정확히 가리키도록
• 히스토리 — 달력 아래 카드 순서를 포토 로그가 먼저 오도록 변경
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
| `build.gradle.kts` | versionCode 6, versionName 1.6.1, APK 출력 이름 `BalletLog-1.6.1.apk` |
| `AndroidManifest.xml` | `READ_MEDIA_IMAGES` 제거 — PhotoPicker가 권한 없이 동작하므로 정책 위반 회피 |

Play Store Share URL: `https://play.google.com/store/apps/details?id=com.hyorita.balletlog`

---

## Play Store (Production) 업로드 절차

1. Play Console → **Production** 트랙 → **Create new release**
2. **App bundle** 에 `app-release.aab` 업로드 (versionCode 5 자동 인식)
3. **Release notes** 에 위 "정식 첫 출시용" 한/영/일 텍스트 붙여넣기
4. **Save → Review release → Roll out to production**
5. 단계 출시 비율 결정 (예: 20% → 50% → 100%) 또는 즉시 100%

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.6.1.apk` (versionCode 6)
3. "원스토어 1.6.1 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

---

## 출시 후 메모

- **원스토어 4명 사용자**: 1.6 사이드로드 받았던 분들. 1.6.1로 업데이트
  받으면 백업 배너 사라지고 Share 섹션 추가됨. 이미 1.6 단계에서 백업
  안내 받은 상태라 자연스럽게 업데이트만 받으면 끝.
- **Play Store production 첫 사용자**: 빈 상태 + 튜토리얼 화살표로 "+"
  버튼 안내. 첫 PhotoLog 작성 시 화살표 자동으로 사라짐.
- **단계 출시 (staged rollout)**: production 첫 출시는 단계 출시로
  20%~50% 먼저 풀고 크래시/리뷰 모니터링 후 100% 권장.

---

## Play Console 검토 중 발견된 이슈 & 해결

### Issue #1 — `READ_MEDIA_IMAGES` 권한 거부 (2026-05-12)

> "사진 및 동영상 권한의 잘못된 사용. 일회성 액세스는 PhotoPicker 사용
> 권장. READ_MEDIA_IMAGES 권한 사용 불가."

**원인**: 우리는 이미 PhotoPicker(`PickMultipleVisualMedia`) 만 쓰고
있는데 manifest에 옛 `READ_MEDIA_IMAGES` 권한 선언이 남아있었음.

**해결**: `AndroidManifest.xml`에서 권한 줄 제거 + versionCode 5 → 6,
AAB / APK 재빌드.

### Issue #2 — 개인정보처리방침에 데이터 삭제 방법 누락 (2026-05-15)

> "데이터 삭제 방법을 명시하지 않음. 사용자의 데이터가 저장/보관되는
> 기간과 사용자가 앱에서 데이터를 삭제할 수 있는 방법을 사용자에게
> 알려야 합니다."

**원인**: Play 정책은 "데이터 수집" 범위에 기기 내부 저장도 포함.
외부 서버에 안 보내도, 사용자가 본인 데이터를 삭제할 수 있는 방법을
정책에 명시해야 함.

**해결**: `balletclass-web/ballet-class-log/app/privacy/page.tsx` (Vercel
배포)에 새 섹션 추가.

1. **Data Retention & Deletion** 섹션 신규 추가 — 데이터 보관 위치(기기
   내부) 명시 + 5가지 삭제 방법:
   - 개별 항목 삭제 (각 상세 화면 휴지통 버튼)
   - 전체 데이터 삭제 (Android: 설정 → 앱 → Storage → Clear data)
   - 앱 제거
   - HealthKit / Health Connect 권한 철회
   - 외부 백업 파일 직접 삭제
2. **Health & Fitness** 카드 보강 — HealthKit(iOS) / Health Connect(Android)
   양쪽 명시 + 심박수 항목 추가.
3. **Third-Party Services** 보강 — Health Connect 함께 명시.
4. **Last updated** May 2026 으로 갱신.

Commit: `balletclass-web` repo `2a597e3` "Add data retention & deletion
section to privacy policy"
