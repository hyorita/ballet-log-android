# 1.16 포팅 계획 — Stats 탭 + History 레이어 (iOS 1.16 sync)

iOS 1.16은 **탭 구조 개편**이다. Stats가 History 자리를 차지해 독립 탭이 되고,
History는 Class 탭 헤더의 캘린더 아이콘에서 여는 레이어로 내려간다. Stats 화면도
같이 전면 리디자인됐다 (스트릭 히어로 카드 · 2×2 지표 · 커스텀 트렌드 차트 · By studio).

기준 문서 (iOS 저장소 `/Users/raulkim/Ballet Log iOS/BalletClassLog 1.2/`):

- `WHATS-NEW-1.16.md` — 사용자 대상 문구 (EN/KO/JA)
- `PRODUCT-NOTES.md` §탭 구조, §Stats — **왜**가 적혀 있음
- `DEV-NOTES.md` §8 — 차트 경과 구간 처리
- `BalletClassLog/Views/StatsView.swift` (v1.16) — 규칙의 원본
- `BalletClassLog/Views/ContentView.swift` — New 배지
- 커밋: `c845c91`(탭 개편) `750f9b2` `9f66ccd` `612696c` `8520315`(Stats) `da0f5a8`(배지) `28cc122`(그리드 배지 버그)

- `screenshots/1.16/01-stats-month.png` — 실제 화면 (Month · 과거 달 · 스트릭 0)

## 화면 (iOS 스크린샷 기준)

`01-stats-month.png` — 2026년 5월(과거 달), 스트릭 0, 수업 9회.

- **배경**은 grouped 회색, 카드는 흰색 · 라운드 16 · 그림자 없음 · 카드 간격 10
- **헤더**: 좌측 회색 `+`(44 터치 영역) · 굵은 `Stats`(title2 semibold) /
  우측 공유 버튼은 흰 원형 배경 위 아이콘
- **세그먼트**: 캡슐형 3칸, 선택 칸만 흰색. 그 아래 `‹  May 2026  ›` — 라벨은 회색
- **스트릭 카드**: 좌상단 `Current streak`(회색) / 우상단 두 줄 우측정렬
  `Longest 4` · `Total 0 kcal` — **라벨은 회색, 숫자만 검정** / 큰 `0` + 회색 `days`
  (baseline 정렬) / 하단 회색 힌트 한 줄. 스트릭 0이어도 `Total 0 kcal`은 숫자로 표시
  (이 줄은 `—` 규칙 대상 아님)
- **2×2**: `9`/Classes · `5h`+`23m`/Workout time · `1,408`+`kcal`/Active energy ·
  `650`+`kcal`/`Hardest · 5/4`. 숫자 크고 검정, 단위·라벨 회색. 셀 사이 hairline이 카드
  끝까지 닿음(여백 없음)
- **트렌드**: 제목 굵게 좌측 · `avg 1.8 / wk` 회색 우측. 과거 달이라 전부 경과 →
  현재 구간 강조 없음, 막대 전부 옅은 핑크(`#FFD8DA`). **값 0인 W2도 라벨 `0`을 표시**하고
  막대는 회색 6dp (라벨 공백은 미래 구간만). 라벨 W1..W5 회색
- **By studio** 카드는 스크롤 아래로 이어짐 (탭바에 가려짐)
- 탭바: Log · Class · Notes · **Stats** · Music (iOS 26 플로팅 탭바 — 안드로이드는
  기존 NavigationBar 유지)

검산: 9회 중 W1 3 · W2 0 · W3 3 · W4 1 · W5 2 = 9, 평균 9/5 = 1.8 ✓

## 1. 탭 구조

`로그 · 클래스 · 노트 · 기록 · 음악` → **`로그 · 클래스 · 노트 · 통계 · 음악`**

| 위치 | iOS | 안드로이드 |
|---|---|---|
| 4번째 탭 | `Stats`, SF `chart.bar.xaxis` | `Screen.Stats` 추가, `Icons.Default.BarChart`(History 헤더에서 쓰던 것) |
| History | 탭에서 제거, **Class 헤더 우측 캘린더 아이콘 → 시트** | `Screen.History` 제거, `ClassScreen` TopAppBar `actions`에 `CalendarMonth` 아이콘 → `ModalBottomSheet { HistoryScreen() }` |
| History 헤더의 Stats 아이콘 | 제거 (이제 중복) | `HistoryScreen.kt`의 `showStats` 시트 · `BarChart` 버튼 제거 |

**배치 기준 (PRODUCT-NOTES):** 반복 방문하는 목적지(Stats)는 탭, 가끔 들춰보는
조회 화면(History)은 레이어. Class 헤더를 고른 이유는 Log 헤더가 `+`/설정으로
이미 차 있어서. 안드로이드 Class 헤더도 현재 `+` 하나라 같은 자리(우측)가 비어 있다.

⚠️ `ClassScreen` 헤더는 지금 `navigationIcon`에 `+`가 있다 — 캘린더는 `actions`(우측)로.

`StatsScreen`은 지금 시트 전제로 짜여 있다 (`onDismiss`, `showMonth(year, month)` 앵커).
탭이 되면서 `onDismiss`와 History→Stats 월 앵커(1.9)는 쓸 곳이 없어진다 — 제거.

**구현 결정 (2026-09-30):**
- History는 `ModalBottomSheet`가 아니라 **전체 화면 레이어**(`Surface` + `BackHandler`,
  헤더 좌측 ←). History 안에 클래스·노트·사진 편집기가 있는데 M3 시트 안의 텍스트
  입력은 IME와 충돌한다. 앱의 다른 오버레이(Detail/Editor)와 같은 방식
- History는 자기 오버레이를 열고 닫을 때 `LocalBottomBarVisible`을 켰다 껐다 한다 —
  레이어 안에서 그대로 두면 History 안의 상세를 닫는 순간 루트 탭바가 History 위로
  되살아난다. ClassScreen이 History에 **별도 플래그**를 provide해서 격리
- Stats 탭은 최고강도·많이 본 수업 탭 시 **자기 안에서** Detail/Editor를 띄운다
  (예전엔 History에 로그를 넘겼음)

## 2. Stats 탭 New 배지

iOS `ContentView`:

```swift
static let statsFeatureVersion = 1
@AppStorage("statsLastSeenVersion") var statsLastSeenVersion = 0
hasNewStats = hasSeenLogTutorial && statsLastSeenVersion < statsFeatureVersion
// 탭 3 선택 시 statsLastSeenVersion = statsFeatureVersion
```

- 배지 텍스트는 숫자가 아니라 **`New`** (ko `New`, ja `新着`)
- **`hasSeenLogTutorial` 게이트가 핵심.** 신규 설치 사용자에겐 Stats가 원래 탭이라
  "New"를 보여주면 안 된다. 첫 로그를 만들 때 켜지는 플래그라 기존 사용자는 이미 true.
  → 안드로이드 `TutorialPreferences.hasSeenLogTutorial()` 그대로 사용
- 날짜가 아니라 버전 정수 — Music 배지(`MusicPreferences`)와 같은 이유. 새
  `StatsPreferences`(SharedPreferences, 키 `statsLastSeenVersion`)로 두면 기존 구조와 맞음
- `MainActivity`에 Music용 `BadgedBox`가 이미 있으니 같은 패턴
- **iOS와 다른 점 (의도적): 기존 사용자 판정을 "기록이 있는가"로.** 튜토리얼
  플래그는 "첫 로그 생성"이 아니라 **Log 탭 `+` 탭** 시점에 켜진다(iOS·안드로이드 동일).
  그래서 ① 신규 사용자가 `+`를 한 번 누르면 다음 실행부터 "New"가 뜨고,
  ② 백업 복원했거나 Class 탭만 쓴 기존 사용자는 플래그가 없어 배지를 못 본다.
  ②는 실기기에서 실제로 확인됨 — 데이터가 많은 debug 앱에 `balletlog_tutorial.xml`이
  아예 없었다. 안드로이드 `StatsPreferences.hasNewStats`는 ClassLog·PhotoLog·Note 중
  하나라도 있으면 기존 사용자, 전부 비어 있으면 **그 자리에서 본 것으로 기록**.
  iOS 1.16.x에서 같은 수정 검토 필요

## 3. Stats 화면 — 섹션 순서

```
헤더:  [+]  통계                    [공유]
       [ 주 | 월 | 년 ]  (세그먼트)
       ‹   9월 1 – 30   ›           (미래 기간이면 › 비활성)
1. 스트릭 히어로 카드     ← 기간과 무관, 항상 "지금" 기준
2. 2×2 지표 카드
3. 트렌드 차트 (1개)
4. By studio              ← 데이터 있을 때만
5. 빈 상태                ← 기간 내 ClassLog가 없을 때
```

헤더 `+`는 **PhotoLog 편집기**를 바로 연다 (ClassLog 편집기 아님).

### 제거되는 것 (안드로이드 현재 코드 기준)

- `MetricsGrid` 아이콘 카드 4개 → 2×2 카드 하나로
- `CubeChart` + `TimeChart` + `YearChart` 3종 → 트렌드 차트 하나로
- **Top viewed** 섹션 (`TopViewedRow`, `StatsAggregates.topViewed`, `stats_top_viewed`)
- (iOS 개발 중간에 있던) **By level** — 최종 빌드에서 드롭됨. 옮기지 말 것

## 4. 스트릭 — 규칙

기간 선택과 **무관**. 전체 기록에서 계산한다.

**클래스 날 집합** = 기간 필터 없이 센 "클래스 이벤트"들의 `startOfDay` 집합.
이벤트 규칙은 기존 Stats 카운팅과 동일해야 한다 (아래 §7 주의).

**현재 스트릭:**
1. 커서 = 오늘. 오늘이 집합에 없으면 **어제**로 한 칸 물림 — 오늘 아직 수업 전이라고
   스트릭이 끊겨 보이면 안 됨
2. 어제도 없으면 0
3. 커서가 집합에 있는 동안 하루씩 뒤로 가며 카운트.
   `mostRecentDate` = 시작 커서, `startDate` = 마지막으로 센 날

**최장 스트릭:** 정렬된 날짜에서 연속 구간의 최대 길이 (비어 있으면 0, 아니면 최소 1).

**스트릭 kcal:** `[startDate, mostRecentDate + 1일)` 범위의 운동 세션 kcal 합.
기존 dedupe된 세션 기준 (§7).

**New record 필:** `current > 0 && current == longest`

**힌트 문구 (4상태, 위에서부터 첫 매치):**

| 조건 | EN | KO | JA |
|---|---|---|---|
| current == 0 | Next class starts a new streak | 다음 클래스부터 새 연속 기록이 시작돼요 | 次のクラスで新しい連続記録が始まります |
| current ≥ longest | Longest streak ever — since %s (`MMMd`) | 역대 최장 기록 — %s부터 | 自己最長記録 — %sから |
| current == longest − 1 | One more day ties your record | 하루만 더 하면 기록과 같아져요 | あと1日で自己記録に並びます |
| 그 외 | Last class %s (`Md`) | 최근 클래스 %s | 直近のクラス %s |

날짜 포맷은 로케일 스켈레톤 (`DateFormat.getBestDateTimePattern(locale, "MMMd")`).

카드 구성: 좌상단 `현재 연속 기록` + (신기록이면) `신기록` 필 / 우상단 `최장 N` ·
`합계 N kcal` / 큰 숫자 `N` + `일` / 하단 힌트 한 줄.

## 5. 2×2 지표 카드

카드 하나 + hairline 구분(세로 1dp · 가로 1dp, `outlineVariant`급). 애플 Fitness 요약 톤.
큰 숫자(`title`급, tabular) + 작은 인라인 단위 + 아래 라벨.

| 셀 | 값 | 비었을 때 |
|---|---|---|
| 수업 | `totalClasses` | `0` 그대로 |
| 운동 시간 | ≥60분: `11h` + `40m`(나머지 0이면 단위 생략) / <60: `40` + `min` | 흐린 `—` |
| 활동 에너지 | `3,420` + `kcal` | 흐린 `—` |
| 최고강도 · 9/12 | 최고 kcal + `kcal`, 탭하면 ClassLog 상세 / PhotoLog 풀스크린 | 흐린 `—`, 라벨은 `최고강도`만 |

**빈 값은 0이 아니라 흐린 `—`** — 진짜 데이터 없음과 0을 구분 (PRODUCT-NOTES).
색은 `onSurface.copy(alpha≈0.3)`처럼 tertiary 톤.

최고강도는 동점이면 ClassLog 우선 (더 풍부한 상세로 연결).

## 6. 트렌드 차트

차트 하나. 기간마다 데이터가 다르다:

| 기간 | 제목 | 막대 | 평균 라벨 |
|---|---|---|---|
| 주 | 일별 운동 시간(분) | 요일별 **운동 분 합** (7개) | `평균 %d분` |
| 월 | 주간 수업 횟수 | W1..W5 **수업 수** (`ceil(일수/7)`개, 1일부터 7일 단위) | `주 평균 %.1f회` |
| 년 | 월간 수업 횟수 | 1..12월 **수업 수** | `월 평균 %.1f회` |

**경과 구간 (`chartElapsedCount`) — 1.16의 핵심 변경:**
- 과거 기간(offset ≠ 0): 전부 경과
- 현재 주: `오늘 − 시작일 + 1` (1..7)
- 현재 월: `(오늘 − 1일) / 7 + 1`
- 현재 년: `현재 월 − 1월 + 1`

경과 안 된 막대는 **회색 6dp placeholder**, 값 라벨은 공백.
**평균은 경과 구간만으로 나눈다** (아직 안 온 W5를 0으로 세면 평균이 희석됨).
최댓값도 경과 구간 안에서만 잡는다.

막대 색:
- 미래 또는 값 0 → 회색(`systemFill` ≈ `onSurface` 8~12%), 높이 6
- 현재 구간(offset == 0일 때 `elapsed − 1`번째) → `#F4B4BA`, 라벨 semibold
- 그 외 → `#FFD8DA`
- 높이 = `max(value / max × 76, 6)`dp, 전체 영역 132dp, 모서리 5

막대 간격: 주 10 · 월 14 · 년 5.

⚠️ **월 차트 주 구분이 다르다 (실기기 확인).** 안드로이드 기존 코드는
`Calendar.WEEK_OF_MONTH`(일요일 시작 달력 주)라 2026-05-07이 **W2**, iOS는
`(일 − 1) / 7`이라 **W1**. 리디자인 때 iOS 방식으로 교체

**년 차트 월 라벨:** 한 글자(`J F M …`). 단 ko/ja처럼 숫자로 시작하는 로케일은
숫자 전체(`10`, `11`, `12`) — 앞 글자만 자르면 10·11·12월이 전부 `1`이 된다.

기존 안드로이드 차트 데이터(`buildTimeData`, `buildCubeData`, `buildMonthlyCounts`)
중 재사용 가능한 것이 있지만, 주 단위 "수업 수" 차트와 월/년 "평균 분" 차트는 이제
쓰지 않는다.

## 7. By studio

- 태그 index 0(studio)을 기간 내 **ClassLog + PhotoLog 양쪽**에서 카운트, 빈 문자열 제외
- 많은 순 정렬
- 행: 이름(1줄) · 우측 숫자 / 그 아래 4dp 진행 막대 (최대값 대비)
- 막대 색은 **핑크가 아니라 `onSurface`** — 비교 데이터는 검정 (다크 모드에서
  사라지지 않도록 literal black 금지). 트랙은 `onSurface` 8%
- 같은 수업이 ClassLog와 같은 날 PhotoLog 양쪽에 태그되면 2로 센다 — iOS가 알고
  받아들인 근사치. 고치지 말 것

## 8. 공유 이미지

iOS는 `ShareableStatsView`를 화면과 **1:1 같은 카드 구성**으로 다시 짰다
(🩰 Ballet Log 헤더 → 스트릭 → 2×2 → 트렌드 → By studio). 안드로이드
`ShareUtils.shareStatsCard`도 새 구성에 맞춰 다시 그려야 한다 — 기존 cube/year
파라미터는 전부 교체. 공유 제목은 `Ballet Log · <기간 라벨>`.

## 9. 문자열

신규 (EN / KO / JA — 값은 iOS `Localizable.xcstrings` v1.16 그대로):

| EN | KO | JA |
|---|---|---|
| Stats | 통계 | 統計 |
| New | New | 新着 |
| Current streak | 현재 연속 기록 | 現在の連続記録 |
| Longest | 최장 | 最長 |
| Total | 합계 | 合計 |
| days | 일 | 日 |
| New record | 신기록 | 自己新記録 |
| By studio | 스튜디오별 | スタジオ別 |
| Classes | 수업 | レッスン |
| Workout time | 운동 시간 | 運動時間 |
| Active energy | 활동 에너지 | 活動エネルギー |
| Hardest | 최고강도 | 最もハード |
| Minutes per day | 일별 운동 시간(분) | 1日あたりの分数 |
| Classes per week | 주간 수업 횟수 | 週のレッスン |
| Classes per month | 월간 수업 횟수 | 月のレッスン |
| avg %d min | 평균 %d분 | 平均%d分 |
| avg %s / wk | 주 평균 %s회 | 週平均%s回 |
| avg %s / mo | 월 평균 %s회 | 月平均%s回 |

+ §4 힌트 4종. 기존 `values*/strings.xml`에 비슷한 키(`stats_*`)가 있으면 값 비교 후 재사용.
`nav_history`는 캘린더 아이콘 contentDescription으로 남긴다.

## 10. 그리드 배지 버그 (`28cc122`) — 포팅 불필요 예상

iOS 원인은 SwiftUI 전용: `Image(resizable+scaledToFill)`에 프레임이 없으면 원본 픽셀
크기를 선호 크기로 보고해 overlay가 밀림. Compose `Box` + `contentAlignment`/`align`
구조에는 해당 없음. **실기기에서 Log 그리드의 하트·kcal 배지 위치만 눈으로 확인**하고 넘어갈 것.

## 11. 카탈로그 시드

`app/src/main/assets/catalog.json` → **v11** (iOS 번들 시드와 바이트 동일,
반주자 61 / 앨범 785). 기존 v8에서 3단계 점프 — 앱 코드 변경 없음 (스키마 v1 그대로).

## 주의 — 기존부터 있던 카운팅 차이

iOS `classEventDates`는 **ID 없는 PhotoLog 운동을 하루 1회로, ClassLog가 있는 날이면
0회로** 센다 (1.15부터). 안드로이드 `dedupeByIdentity`는 `externalWorkoutId`로만
dedupe해서, 같은 날 ID 없는 사진 운동이 추가로 잡힐 수 있다.

스트릭은 "날" 집합이라 영향 없지만, **수업 수 · 트렌드 막대**는 iOS와 달라질 수 있다.
→ **1.16에서 맞춘다 (결정 2026-09-30).** iOS는 두 규칙을 따로 쓴다:

- **수업 수** (`classEventDates`): ClassLog는 전부 1회씩(운동 데이터 없어도).
  사진 운동 중 ID 있는 것은 ClassLog와 ID가 겹치지 않으면 1회, ID 없는 것은
  ClassLog·ID 있는 운동이 없는 날에 하루 1회
- **운동 시간 · kcal** (`workoutSessions`): 운동 데이터 있는 ClassLog + 사진 운동.
  1차로 ID로 dedupe, 2차로 ID 없는 것은 이미 덮인 날을 건너뛰고 하루 1회
- **사진 운동 판정**은 `kcal != null || durationMin != null` (안드로이드
  `hasWorkoutData`는 BPM까지 봐서 더 넓다 — Stats에서는 iOS 기준을 쓴다)
- 최고강도는 dedupe 없이 전체 후보 중 최대, 동점이면 ClassLog

## 착수 전 확인

- [x] iOS 1.16 Stats 스크린샷 — Month(과거 달, 스트릭 0) 1장. Week/Year · 진행 중 스트릭 ·
      신기록 상태는 필요해지면 추가
- [x] §7 카운팅 차이 — 맞춘다
- [ ] `main`이 origin보다 4커밋 앞서 있음 (1.14 · 1.15) — push 여부

## 상태

- [x] `dev/1.16` 브랜치
- [x] versionCode 17 / versionName 1.16
- [x] `assets/catalog.json` 시드 v11
- [x] 카운팅 규칙 iOS 정렬 (§주의) — `StatsCountingTest` 13건
- [x] 탭 개편 (Stats 탭 · History 레이어 · Class 헤더 캘린더 아이콘)
      — 실기기 확인 (2026-09-30, SM-S926N / Android 16): 탭 5개 · Class 헤더 캘린더 →
      History 전체 화면(←, 탭바 숨김) · History 안 수업 상세 열고 닫아도 탭바 안 살아남 ·
      History 안 노트 편집기 IME 정상(칩이 키보드 바로 위) · 뒤로가기 단계별 복귀 →
      Class 탭에서 탭바 복귀 · Stats 탭 헤더 상태바 여백 정상.
      Stats→수업 상세 경로는 테스트 데이터상 진입점이 없어 미확인(최고강도가 사진 운동,
      조회수 0) — 리디자인 후 재확인
- [x] New 배지 (`StatsPreferences` + 기록 유무 게이트) — 실기기 확인: 기록 있는 앱에 `New`, 탭 열면 사라지고 `statsLastSeenVersion=1` 저장
- [ ] StatsViewModel — 스트릭 · 경과 구간 · By studio · Top viewed 제거
- [ ] StatsScreen — 헤더 `+` · 히어로 카드 · 2×2 · 트렌드 · By studio
- [ ] 공유 이미지 재작성
- [ ] EN/KO/JA 문자열
- [ ] 단위 테스트 (스트릭: 오늘 없음→어제 기준 / 끊김 / 최장 / 경과 구간 / 월 라벨 로케일)
- [ ] 실기기: Log 그리드 배지 위치 확인
- [ ] PLAY-CONSOLE-1.16.md
