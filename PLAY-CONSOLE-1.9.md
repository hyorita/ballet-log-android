# Ballet Log 1.9 — 업로드 자료

versionCode 10 / versionName 1.9

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.9.apk`
- Play Store: 필요 시 `./gradlew :app:bundleRelease` 로 AAB 생성

iOS 1.9 동기화. Apple Health / Apple Watch → Android Health Connect 대응.

---

## 1.9 Release Notes (Play Store / 원스토어 공용)

짧은 버전. 스토어 "이번 버전의 새로운 기능" 필드에 그대로 붙여넣기.

### 한국어

```
✨ 1.9
• 지난 워크아웃 불러오기 — Health Connect 연결 시, 최근 30일 중 기록 안 된 운동을 한 번에 추가.
• 히스토리에서 추가 — 날짜를 골라 미기록 운동을 탭하면 바로 기록.
• 통계 정리 — 불러온 운동도 반영되고, 같은 운동은 한 번만 집계.
```

### English

```
✨ 1.9
• Bring in past workouts — connect Health Connect to add the last 30 days of un-logged workouts in one tap.
• Add from History — pick a day and tap any un-logged workout to log it.
• Tidier stats — imported workouts count too, each only once.
```

### 日本語

```
✨ 1.9
• 過去のワークアウトを取り込む — Health Connect 接続で、直近30日の未記録の運動をまとめて追加。
• ヒストリーから追加 — 日付を選んで未記録の運動をタップで記録。
• 統計の整理 — 取り込んだ運動も反映、同じ運動は一度だけ集計。
```

---

## 1.9 변경사항 (개발자 메모)

워크아웃 수동 import(History) + 연결 시 backfill + 워크아웃 신원(UUID) dedup + Stats 일관성.
1.8에서 이미 `PhotoLog.externalWorkoutId` + auto-import을 구현해뒀기에, 1.9는 그 위에 수동/일괄 import UX와 Stats 반영을 얹음. **Room 스키마 변경 없음(version 6 유지) → 백업 1.8 ↔ 1.9 양방향 안전.**

| Phase | 항목 | 변경 |
|---|---|---|
| A | `HealthConnectManager.kt` | `scanWorkouts(start, end)` 범위 쿼리 추가, `scanRecentWorkouts`가 위임. `readWorkoutForDate`가 `externalWorkoutId` 채움. |
| A | `WorkoutInfo.kt` | `externalWorkoutId: String?` 추가 (Gson blob 내부 → 마이그레이션 불필요). |
| A | `HealthConnectAutoImport.kt` | 공용 `importWorkouts(dao, records)` dedupe+insert 경로 추출 (auto/History/backfill 공유). `PhotoLogDao.getAllExternalWorkoutIds()` 추가. |
| B | `BackfillPreferences.kt` (신규) | 일회성 "prompted" 플래그 (응답 시에만 set). |
| B | `MainActivity.kt` | 첫 실행 + HC 권한 시 30일 미로그 워크아웃 일괄 추가 `AlertDialog`. |
| C | `HistoryScreen.kt` | 보던 달 스캔 → 미로그 워크아웃 추출. 월 배너(모두 추가/닫기/다시 보지 않기) + 일별 탭 가능한 추가 행. |
| C | `HistoryPreferences.kt` (신규) | 배너 "다시 보지 않기" 영구 플래그. |
| C | `PhotoLogViewModel.kt` | `scanWorkouts` / `importWorkouts` 래퍼. |
| D | `StatsViewModel.kt` | ClassLog 워크아웃 + 워크아웃 PhotoLog placeholder 통합 `StatItem` 집계, `externalWorkoutId` 2-pass dedup. `showMonth()` 앵커. |
| D | `StatsScreen.kt` / `ShareUtils.kt` | "Hardest Class" → "Hardest Workout", 공유 카드 시그니처 calories/date 분리. History에서 열면 보던 달 앵커. |
| E | `util/DebugLog.kt` (신규) | `debugLog()` — `BuildConfig.DEBUG` 게이트, release 컴파일아웃. 12개 `Log.*` 호출 치환. `buildConfig = true`. |
| F | `strings.xml` (en/ko/ja) | `backfill_*`, `history_unlogged_*` 신규. |
| F | `build.gradle.kts` | versionCode 10, versionName 1.9. |

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.9.apk` (versionCode 10)
3. 위 "원스토어 1.9 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

---

## Play Store 업로드 (필요 시)

```bash
./gradlew :app:bundleRelease
# 출력: app/build/outputs/bundle/release/app-release.aab
```

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.

> ⚠️ versionCode 10은 어떤 트랙에든 업로드되는 순간 영구 점유됨. 베타에서 버그 발견 시 재사용 불가 → 1.9.1 (versionCode 11)로 bump. (1.8 → 1.8.1 전례 참조)
