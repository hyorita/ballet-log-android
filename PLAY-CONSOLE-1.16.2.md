# Ballet Log 1.16.2 — 업로드 자료

versionCode 19 / versionName 1.16.2

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.16.2.apk`
- Play Store: `app/build/outputs/bundle/release/app-release.aab`

```bash
./gradlew :app:assembleRelease :app:bundleRelease   # APK·AAB 같은 시점에 같이
```

iOS 1.16.2 동기화. 할로윈 한정 앱 아이콘 — 10/29~10/31에만 자동으로 바뀌고, 11/1부터
원래 아이콘으로 돌아옴.

---

## 1.16.2 Release Notes (Play Store / 원스토어 공용)

스토어 "이번 버전의 새로운 기능" 필드에 그대로 붙여넣기. iOS `WHATS-NEW-1.16.2.md`와 같은 문구.

### 한국어

```
✨ 1.16.2
• 할로윈 한정 앱 아이콘 — 10월 29일부터 31일까지만 자동으로 바뀌었다가, 다시 원래 아이콘으로 돌아와요.
```

### English

```
✨ 1.16.2
• A Halloween app icon for a few days — the icon automatically switches October 29-31, then back to normal.
```

### 日本語

```
✨ 1.16.2
• 期間限定のハロウィンアイコン — 10月29日から31日の間だけ自動で切り替わり、終わるといつものアイコンに戻ります。
```

---

## 1.16.2 변경사항 (개발자 메모)

| iOS 항목 | Android 대응 |
|---|---|
| `AppIcon-Halloween` 에셋 + `ASSETCATALOG_COMPILER_ALTERNATE_APPICON_NAMES` | ✅ `<activity-alias>` 2개 (`LauncherDefault`/`LauncherHalloween`), 평소엔 Default만 `enabled` |
| `UIApplication.setAlternateIconName` (scenePhase active 때마다 체크) | ✅ `PackageManager.setComponentEnabledSetting` — `MainActivity.onStart()`에서 체크 |
| 전환 시 시스템 "아이콘이 변경되었습니다" 알림 (끌 수 없음) | 없음 — Android는 조용히 전환, 알림 없음 |

**Room 스키마 변경 없음. 새 권한·새 데이터 수집 없음** — 런처 아이콘만 바뀜.

**리뷰어 참고 (심사 노트에 적을 내용):** 10/29~10/31 바깥 날짜에 설치/심사하면 평소 아이콘만
보임 — 의도된 동작. 전환은 로컬 기기 날짜 기준이라 리뷰어 서버 시간대와 무관하게 동작함.

---

## 출시 전 확인

- [x] **iOS 1.16.2가 App Store 심사를 통과했는지** — 확인 필요 (iOS는 당일 승인 전례)
- [x] release AAB/APK 빌드 — 2026-10-28 16:32, versionCode 19 확인
- [x] 개인정보처리방침 URL — 변경 없음
- [x] 데이터 세이프티 섹션 — 변경 없음 (앱이 새로 수집하는 데이터 없음)

> versionCode 19는 어떤 트랙에든 업로드되는 순간 영구 점유됨. 이후 버그 발견 시 1.16.3
> (versionCode 20)으로 bump.

> 할로윈 윈도우(10/29~31)가 이미 진행 중이므로, 업로드→심사→배포가 늦어질수록 실제로
> 할로윈 아이콘을 보는 사용자가 줄어듦 (11/1부턴 평소 아이콘으로 자동 복귀).

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.16.2.apk` (versionCode 19)
3. 위 "1.16.2 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

## Play Store 업로드

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.
