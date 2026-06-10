# Ballet Log for Android — 1.9 개발 계획

iOS `BalletClassLog 1.2 / WHATS-NEW-1.9.md` 기준 포팅. 사용자 가시 동작·릴리스 카피를 미러, 구현은 Android idiom.

- 출발점: dev/1.8 (1.8.1, versionCode 9) → 새 브랜치 `dev/1.9`
- 목표 버전: versionName `1.9`, versionCode `10`
- ⚠️ dev/1.8이 main보다 11커밋 앞섬 — 1.8/1.8.1이 아직 main 미머지. (릴리스 ops 별개 정리 필요)

## 1.9 = "지난 워크아웃 불러오기"

iOS 1.9 핵심 3가지:
1. **연결 시 backfill** — Health 연결됨 + 미제안이면 최근 30일 미로그 워크아웃 일괄 추가 제안 (신규+기존 사용자 1.9 첫 실행).
2. **History 수동 import** — 날짜 선택 시 그날의 미로그 워크아웃을 인라인 행 → 탭하면 추가. 월 단위 "미 기록 N개" 배너 카드(추가 / 닫기 / 다시 보지 않기).
3. **워크아웃 신원(UUID) dedup + Stats 일관성** — 같은 운동은 한 번만 카운트.
4. 내부 정리: 로깅 조용히(release 컴파일아웃).

## 현재 Android 상태 (1.8에서 이미 된 것)

- `PhotoLog.externalWorkoutId` (= iOS `healthKitWorkoutID`) **있음** — Room v6, MIGRATION_5_6, index 포함.
- `HealthConnectManager.scanRecentWorkouts(lookbackHours=24)` → `List<ScannedWorkout>`(externalId/start/end/duration/kcal/avg·maxBpm).
- `HealthConnectManager.readWorkoutForDate(date)` → `WorkoutInfo?` (단, **externalId 없음**) — ClassLog 에디터용.
- `HealthConnectAutoImport.importRecent()` — scan → `findByExternalWorkoutId` dedup → placeholder PhotoLog insert. 포그라운드 onStart에서 60s throttle 트리거.
- Settings에 Health Connect 권한 행, MainActivity onCreate 권한 요청.
- **결론: 워크아웃 신원 dedup의 auto-import 경로는 사실상 1.8에서 완료.** 1.9는 그 위에 수동/일괄 import UX + Stats 반영을 얹는 작업.

## 갭 분석 & 작업 (Phase 단위 — 커밋 컨벤션 "1.9 Phase X: ...")

### Phase A — Health Connect 범위 스캔 + 공용 import 헬퍼 (기반)
- `HealthConnectManager.scanWorkouts(context, startMillis, endMillis): List<ScannedWorkout>` 추가 — `scanRecentWorkouts`를 일반화(범위 쿼리). 30일 backfill·월간 History 둘 다 사용.
- `WorkoutInfo`에 `externalWorkoutId: String? = null` 추가 — **Gson blob(workoutJson) 내부 필드 → Room 스키마 변경 없음, 마이그레이션 불필요, 백업 양방향 안전.** `readWorkoutForDate`가 이 값 채우도록.
- 공용 `HealthConnectAutoImport.importWorkouts(dao, records): Int` 추출 — auto / backfill / History 3경로 모두 같은 dedup+insert. (현재 inline 로직을 헬퍼로.)
- dedup 대상 externalId 집합: PhotoLog.externalWorkoutId ∪ (ClassLog.workout?.externalWorkoutId). PhotoLogDao에 `getAllExternalWorkoutIds(): List<String>` 추가(또는 기존 Flow에서 in-memory set).

### Phase B — 연결 시 일회성 backfill 프롬프트
- `BackfillPreferences` (SharedPreferences object, idiom: ProfilePreferences) — `backfillPrompted` 플래그.
- 앱 루트(BalletLogApp) Compose `AlertDialog`: 스플래시 직후, HC 권한 있음 && !prompted → 최근 30일 스캔, 미로그 N개 카운트 → "지난 30일 워크아웃 N개를 추가할까요?" [추가] [나중에]. **응답 시에만** 플래그 set.
  - iOS는 TabView .alert 버그로 커스텀 오버레이 우회했지만 Android는 표준 AlertDialog로 충분(플랫폼 차이).
- [추가] → `importWorkouts` 일괄 insert, 결과 토스트("N개 추가됨") 선택.

### Phase C — History 수동 import
- HistoryScreen 선택일에 그날의 **미로그 워크아웃 인라인 행** (HealthConnect 범위 스캔 결과 − 이미 로그된 externalId) → 탭하면 placeholder insert (Phase A 공용 경로). 새 컴포저블 `HistoryUnloggedWorkoutRow`.
- 월 뷰 상단 **배너 카드** "미 기록된 활동 N개" [로그에 추가] [닫기] [다시 보지 않기]. 월별 dismiss + 글로벌 "다시 보지 않기" → SharedPreferences. 새 `HistoryUnloggedBanner`.
- 월 단위 스캔은 캘린더 month 범위로 `scanWorkouts(monthStart, monthEnd)`.

### Phase D — Stats 일관성  ⚠️ 설계 결정 필요
- 현재 Android Stats(StatsViewModel)는 **ClassLog만** 집계. PhotoLog 워크아웃 placeholder는 stats에 안 들어옴 → iOS "placeholder 반영"과 divergence.
- 결정 사항: Android stats를 워크아웃 소스 인지로 만들지 (ClassLog 워크아웃 + PhotoLog placeholder, externalWorkoutId 2-pass dedup) vs. rename만.
- 확정 시 작업: 워크아웃 세션 집계에 PhotoLog placeholder 포함 + externalId dedup(같은 운동 두 로그=1, 같은 날 다른 운동 2개=2).
- 라벨: `hardestClass` → `hardestWorkout`("Hardest workout"). History에서 열 때 보던 달 기준(referenceDate) 앵커. (이전기간 비교는 iOS에 있음 — Android 현황 확인 후 포함 여부.)

### Phase E — 로깅 정리
- `debugLog()` 헬퍼(BuildConfig.DEBUG 게이트) — `Log.d`/`println` 치환, release no-op. ("Under the hood — quieter logging" 카피 대응.)

### Phase F — 릴리스 준비 (마지막)
- versionName `1.9`, versionCode `10` bump (검증 통과 후).
- `PLAY-CONSOLE-1.9.md` 작성 — WHATS-NEW-1.9 ko/en/ja 카피 포팅(PLAY-CONSOLE-1.8.1.md 포맷 템플릿). "Apple Health"→"Health Connect", "Apple Watch"→"기기"로 치환.
- 빌드 검증 + APK(`BalletLog-1.9.apk`).

## 데이터/안전
- Room 버전 **6 유지, 새 마이그레이션 없음** (WorkoutInfo 필드는 blob 내부, PhotoLog.externalWorkoutId는 1.8에 이미 있음).
- 백업/복원 1.8 ↔ 1.9 양방향 안전.

## 확정된 결정
1. **Stats 범위 (Phase D): 충실 포팅** — PhotoLog placeholder까지 워크아웃 소스 인지 + externalWorkoutId 2-pass dedup (iOS 동일: 같은 운동 두 로그=1, 같은 날 다른 운동 2개=2). `hardestClass`→`hardestWorkout` 리네임 포함. (2026-06-10 결정)
