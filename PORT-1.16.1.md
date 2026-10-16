# 1.16.1 포팅 계획 — 발레 페르소나 웹 퀴즈 진입점 (iOS 1.16.1 sync)

iOS 1.16.1은 기능 하나다: balletclass.cc의 **"내 발레 페르소나 찾기"** 웹 퀴즈
(8문항 → 16가지 페르소나)로 들어가는 앱 내 진입점 3곳.

기준 (iOS 저장소 `/Users/raulkim/Ballet Log iOS/BalletClassLog 1.2/`):

- `97b312c` 진입점 추가 (설정 행 · Log 배너 · 배지) — `Utilities/MbtiLink.swift`,
  `Views/PhotoLogView.swift`, `Views/SettingsView.swift`
- `aadac92` 배너를 첫 오픈 후 숨김 (2026-10-15 결정, 아래)
- `44fa989` 1.16.1 (build 1) + `WHATS-NEW-1.16.1.md`
- 웹: `/Users/raulkim/balletclass-web/ballet-class-log` — `app/mbti`, `lib/mbti/appUnlock.js`

## 진입점

| 위치 | iOS | 안드로이드 |
|---|---|---|
| **설정 맨 위 행** | `sparkles` + "Find My Ballet Persona" + (기존 사용자) `New` 캡슐 + 셰브런, footer에 설명 | `SettingsRow`(+ `badge` 파라미터 추가), `Icons.Default.AutoAwesome`, footer `mbti_subtitle` |
| **Log 탭 배너** | 스크롤 맨 위, 태그 필터 위. 튜토리얼 중엔 숨김. 기록 0개(빈 상태)에서도 표시 | `PhotoLogGrid(header =)` 첫 아이템 / 빈 상태면 `EmptyState` 위 |
| **Log 톱니 빨간 점** | 기존 사용자 · 미확인 시 8pt 빨간 점 | `BadgedBox` + `Badge(Color.Red)` (내용 없는 점) |

## 반드시 지킬 것

1. **URL은 웹과 바이트 단위로 같아야 한다**
   `https://www.balletclass.cc/mbti?x7h=8aq6brde1s`
   `x7h=8aq6brde1s`는 결과 화면의 아트 스타일 잠금을 전부 푼다 — 앱 안에서 누른 사람은
   이미 설치했으니 "앱 설치하면 열림" 캐러셀이 팔 게 없다. **원본은 웹 `lib/mbti/appUnlock.js`**.
   보안 장치가 아니라 "사람이 외우거나 추측하기 어려운 값"일 뿐이다 (APK를 열면 보임 — 의도된 수준)
2. **배지 규칙 = 1.16 Stats 배지와 동일.** 기록(ClassLog·PhotoLog·Note)이 있는 기존
   사용자만, 기록 없는 설치는 첫 실행에 "봤음"으로 시드. 공용 `hasAnyRecords()`로 분리
   (`data/ExistingUser.kt`) — `StatsPreferences`도 이걸 쓰도록 바꿈
3. **설정의 New 캡슐은 스냅샷.** 톱니를 누르는 순간 점을 지우므로, 설정 화면이 실시간
   값을 읽으면 이미 지워진 뒤다. iOS와 같이 탭 직전 값을 `showMbtiBadge`로 넘긴다
4. **배너는 첫 오픈 후 사라진다 (닫기 X 없음).** 원래 iOS `97b312c`는 캠페인 기간 동안
   계속 고정(누르기만 하면 그대로)이었는데, 2026-10-15 사용자 결정으로 변경: 누른 사람은
   이미 퀴즈를 봤고, 고정 배너는 Log 그리드를 매번 밀어낸다. 다시 찾는 길은 설정 행의
   New 캡슐이 안내. 새 설치 사용자에게도 보임(앱 사용자 대상 프로모션이라 의도)
5. 배너가 사라져도 열린 퀴즈는 닫히지 않아야 한다 — iOS는 시트가 화면 루트에 붙어 있고,
   안드로이드는 Custom Tab이라 별도 액티비티라서 애초에 영향 없음

## 안드로이드 전용 결정

**인앱 브라우저 = Chrome Custom Tabs** (`androidx.browser:browser:1.8.0` 추가). iOS
SafariView 대응 — 앱을 떠나지 않고 뒤로가기로 돌아온다. 앱의 다른 외부 링크(유튜브,
Apple Music 등)는 지금처럼 `ACTION_VIEW` 유지. 브라우저가 아예 없으면 조용히 무시.

**색**: iOS `Accent`(라이트 검정 / 다크 `#FFC0CB`) = 안드로이드 `colorScheme.primary`
(BalletBlack / BalletPink)와 동일 → 배너 아이콘·New 캡슐 배경에 `primary`, 캡슐 글자는
`onPrimary` (iOS는 흰색 고정이라 다크 모드에서 분홍 위 흰 글자 — 안드로이드가 대비 더 나음).

## 문자열

iOS `Localizable.xcstrings` 그대로: `mbti_title` · `mbti_subtitle` · `mbti_new_badge`
(ja는 `新着` — Stats 배지와 동일).

## 상태

**2026-10-16 출시** (App Store · Google Play · 원스토어). 태그 `v1.16.1` = `6d07cce`.


- [x] `dev/1.16.1` 브랜치
- [x] versionCode 18 / versionName 1.16.1
- [x] 카탈로그 시드 — iOS와 바이트 동일 확인 (변경 없음, v11)
- [x] Custom Tabs 의존성 · `openInAppBrowser()`
- [x] `MbtiPreferences` · `hasAnyRecords()` 공용화
- [x] 설정 행 + New 캡슐 / Log 배너 / 톱니 빨간 점
- [x] EN/KO/JA 문자열
- [x] 빌드 · 기존 단위 테스트 40건 통과
- [x] 실기기 (2026-10-15, SM-S926N / Android 16, debug, 기록 있는 앱): 배너가 태그 필터 위 ·
      톱니 빨간 점 → 설정 `New` 캡슐(설정을 연 동안 유지) → 닫으면 점 없음 · 설정 행 →
      Custom Tab(`CustomTabActivity`) 열림 · 뒤로 복귀 · 배너 탭 → 퀴즈 열림 → 복귀 시 배너 사라짐
      (`hasDismissedMbtiBanner=true`). 설정에서 연 것으로는 배너가 안 사라짐(iOS와 동일)
- [x] **확인 중 수정**: 설정 행 제목이 `내 발레 / 페르소나 찾기` 두 줄로 꺾임 — 제목과 빈 칸이
      weight를 반씩 나눠 가짐. 제목+캡슐을 안쪽 Row로 묶어 제목이 캡슐 뺀 나머지를 전부 쓰게 함
- [x] `x7h` 전달 — 웹 `middleware.js`가 en/ja/zh 브라우저를 `/mbti/<locale>`로 307
      리다이렉트하는데 쿼리를 보존함(`curl`로 `location: /mbti/en?x7h=8aq6brde1s` 확인).
      ko는 리다이렉트 없이 `/mbti` 그대로. 결과 화면까지 직접 풀어보진 않음
- [ ] 새 설치(기록 0) 시 점·캡슐 안 뜨는 경로 — 1.16 Stats와 같은 코드라 실기기 미확인
- [ ] 다크 모드
- [x] PLAY-CONSOLE-1.16.1.md
