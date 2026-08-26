# 1.13 포팅 계획 — Music 탭 (iOS 1.13 sync)

iOS 1.13은 기능 하나다: **Music 탭**. 발레 클래스 반주 음악 카탈로그
(15개국 반주자 61명 / 앨범 780장)를 앱 안에서 둘러보고, 30초 미리듣기로
들어본 뒤 Apple Music으로 넘어가는 콘텐츠 탭.

기준 문서 (iOS 저장소, 읽는 순서대로):

- `/Users/raulkim/Ballet Log iOS/BalletClassLog 1.2/ANDROID-PORT.md` — **왜**가 적혀 있음. 먼저 읽을 것
- `.../catalog/CATALOG-SCHEMA.md` — 데이터 계약
- `.../WHATS-NEW-1.13.md` — 사용자 대상 문구 (EN/KO/JA)
- `.../screenshots/1.13/01-music.png`, `02-album.png` — 실제 화면

## 데이터

카탈로그는 **iOS와 공유하는 자산**이다. 안드로이드용으로 포크하지 말 것.

```
https://hyorita.github.io/ballet-log-catalog/v1/catalog.json          (378KB, 확인됨)
https://hyorita.github.io/ballet-log-catalog/v1/albums/<albumId>.json (2~10KB)
```

2026-08-16 기준 라이브 확인: `schemaVersion 1`, `catalogVersion 8`,
artists 61 / albums 780 (tier 1 = 32명). 앨범 상세도 200 응답 정상.

시드 파일을 `app/src/main/assets/catalog.json`에 이미 넣어 뒀다
(iOS 번들 시드와 바이트 동일). 로딩 순서는 **캐시 → 번들 시드 → 원격 갱신**,
원격 `catalogVersion`이 더 크면 캐시 교체.

## 옮긴 파일

| iOS | 안드로이드 |
|---|---|
| `Models/MusicCatalog.swift` (161줄) | `data/model/MusicCatalog.kt` — Gson 데이터 클래스 |
| `Services/CatalogService.swift` (162줄) | `data/CatalogRepository.kt` |
| `Services/AlbumFavorites.swift` (35줄) | `data/AlbumFavoritesPreferences.kt` |
| `ContentView.swift`의 배지 상태 | `data/MusicPreferences.kt` + MainActivity |
| `Services/PreviewPlayer.swift` (62줄) | `ui/music/PreviewPlayer.kt` |
| `Services/ArtworkLoader.swift` | 불필요 — Coil이 대체 (디스크 캐시가 있어 iOS보다 유리) |
| `Views/MusicView.swift` (565줄) | `ui/music/MusicScreen.kt` |
| `Views/ArtistDetailView.swift` (125줄) | `ui/music/ArtistDetailScreen.kt` |
| `Views/AlbumDetailView.swift` (238줄) | `ui/music/AlbumDetailScreen.kt` |

MusicScreen 섹션 순서 (iOS와 동일): 오늘의 발견 → 새로 나온 앨범 →
보관함(있을 때만) → 반주자 목록. 검색 중이면 검색 결과로, 검색창에
포커스만 있고 입력이 없으면 제안 칩으로 교체된다.

탭 헤더는 `Ballet Class Music`, 탭 라벨은 `Music`. iOS가 일부러 다르게
뒀다 — "Music" 하나만 두면 음악 앱처럼 읽힌다. Log 탭이 이미 쓰는 분리
방식(탭 `Log` / 헤더 `Ballet Log`)과 같다.

## 의존성 — 하나도 추가하지 않았다

계획 단계에선 Media3 ExoPlayer · DataStore · OkHttp 3종을 예상했는데, 셋 다
이 기능의 크기에 비해 과했다.

- **미리듣기 → `MediaPlayer`.** 한 번에 한 곡, 큐 없음, 백그라운드 재생 없음,
  노출할 세션 없음. ExoPlayer가 주는 버퍼링 정책·트랙 선택·미디어 세션이
  전부 안 쓰이는 자리라 APK만 무거워진다. 30초 AAC/MP4 스트림 하나면 충분
- **즐겨찾기 → SharedPreferences.** 앱의 다른 설정(`CollapsedMonthsPreferences`
  등)이 전부 이 방식이라 맞췄다. iOS의 `UserDefaults`와도 같은 층위
- **카탈로그 fetch → `HttpURLConnection`.** 정적 JSON 2종, 인증 없음,
  인터셉터 없음

Coil · Gson · Navigation은 이미 있던 것을 그대로 썼다.

## 안드로이드 전용 결정

**유튜브뮤직 검색 링크를 함께 둔다.** 국내 안드로이드 사용자 중 Apple Music
비율이 낮다. 다만 **Apple Music 링크를 빼면 안 된다** — 아트워크와 미리듣기가
Apple 자산이라 표시 요건 문제가 된다. 둘 다 노출. 새 데이터 없이 URL 조립:

```kotlin
val q = Uri.encode("${artist.name} ${album.title}")
"https://music.youtube.com/search?q=$q"
```

**탭 배치 — iOS와 동일하게 맨 뒤.** 5탭이 된다:
로그 · 클래스 · 노트 · 기록 · **음악**. 아이콘은 음표(`Icons.Default.MusicNote`).
개인 기록 탭 무리를 가르지 않는다는 제약도 자동으로 지켜진다.

## 화면 (iOS 스크린샷 기준)

`screenshots/1.13/01-music.png` · `02-album.png` · `03-artist.png` 확인 결과.

**Music (01)** — 헤더 `발레 클래스 음악`.
- `오늘의 발견` — 카드 하나. 좌측 아트워크 132pt, 우측에 앨범명(2줄) ·
  반주자명 · `affiliation`(작게) · `note`(3줄). 카드 배경 + 라운드 14 + 옅은 그림자
- `새로 나온 앨범` — 가로 스크롤 타일. 아트워크 정사각 + 제목(2줄) + 반주자명
- `보관함` — 하트 누른 앨범이 있을 때만, 같은 가로 타일
- `반주자` — 세로 리스트 행: 썸네일 · 이름 · `affiliation` · `앨범 42장` · 셰브런.
  기본 노출은 일부, `더 보기`로 확장
- **검색창이 화면 맨 아래**(탭바 바로 위)에 고정. 안드로이드도 동일하게 하려면
  `Scaffold`의 `bottomBar`에 검색창 + NavigationBar를 겹쳐 쌓는 구조가 된다

**앨범 상세 (02)** — 상단 뒤로 + 앨범명.
- 아트워크(약 250) 우측에 앨범명 · 반주자명 · `affiliation` · `2026 · 30곡`
- 액션 행: `♡ 저장하기` / `↗ Apple Music에서 열기` — **여기에 유튜브뮤직 항목 추가**
- 그 아래 회색 한 줄: `30초 미리듣기입니다. 전체 듣기는 Apple Music에서.`
- 트랙 행: ▶ 원형 버튼 · 제목 · 동작 칩(`Warm Up` `Plié` `Tendu` `Dégagé` …) ·
  우측 원곡 길이(`2:44`). `exercise`가 null이면 칩 없음

**반주자 상세 (03)** — 이름 · `affiliation` · `note` 전문 ·
`↗ Apple Music에서 열기`(여기도 유튜브뮤직 추가) · `앨범` 2열 그리드
(아트워크 + 제목 2줄 + 발매연도).

## 반드시 지킬 제품 결정

`ANDROID-PORT.md` §2에 이유가 적혀 있다. 코드만 보면 "개선"하고 싶어지는
것들이라 옮겨 적어 둔다.

1. **국가 코드·국기를 화면에 표시 금지.** `region`은 검색 키 전용, 화면에는
   `affiliation`. 61명 중 9명이 출신국과 활동국이 달라 국가 칩은 국적을 틀리게 말한다
2. **앨범 수로 정렬 금지.** `rank`(큐레이션 순)로 정렬
3. **A-Z 정렬을 기본으로 두지 말 것.** 기본은 큐레이션 순
4. **오늘의 발견은 날짜 시드로 하루 고정** — `hash(yyyy-MM-dd) % tier1앨범수`.
   풀은 티어 1 + 트랙 5곡 이상
5. **30초 미리듣기임을 화면에 밝힐 것.** 트랙 행에 원곡 길이(2:44)가 뜨는데
   재생은 30초에 끊긴다. 안 적으면 버그로 읽힌다
6. **검색은 모든 로케일의 텍스트를 볼 것.** 현재 언어만 보면 "vaganova"가
   바가노바 반주자 2명을 놓친다
7. **실패하면 조용히.** 카탈로그를 못 받으면 그 섹션만 안 보이면 된다. 에러 표시 금지
8. **검색 제안 칩은 축을 섞을 것** (프랑스 · 바가노바 · 팝 · 쇼팽)
9. **필터는 같은 축 안에서 OR, 축이 다르면 AND.** 1.12 Log 태그 필터는
   전부 AND라 규칙이 다르다 — 재사용 시 주의
10. **아트워크를 재배포하지 말 것.** Apple CDN URL을 그대로 로드하고
    Apple Music 링크를 함께 노출

`affiliation` · `note`는 로케일 딕셔너리. 폴백은 `현재 로케일 → en → 첫 값`.
앱 문구를 새로 쓰지 말고 카탈로그 값을 그대로 표시할 것.

## 옮기지 않는 것

- iOS DEBUG 진입 인자(`-BLTab`, `-BLAlbum`, `-BLSearch`) — `adb shell input tap`이
  되므로 불필요

## 착수 전 확인

- [ ] Play 스토어 개인정보처리방침 URL — Google Play는 URL 하나만 받으므로
      언어 선택이 있는 페이지를 넣을 것
- [ ] Apple 아트워크·미리듣기 표시 요건 현행 확인 (iOS 문서도 "착수 전 확인" 상태)
- [ ] `catalogVersion` 갱신 주기 — 월간 자동화는 iOS 쪽에서 아직 미결

## 상태

- [x] `dev/1.13` 브랜치
- [x] versionCode 14 / versionName 1.13
- [x] `app/src/main/assets/catalog.json` 시드 동봉
- [x] 모델 · 리포지토리 · 즐겨찾기 · 배지 상태
- [x] MusicScreen · ArtistDetail · AlbumDetail
- [x] 미리듣기 플레이어
- [x] 탭 추가 (5탭, 맨 뒤)
- [x] EN/KO/JA 문자열
- [x] 단위 테스트 (`MusicCatalogTest` — 시드 파싱, FNV-1a 일치, 오늘의 발견 규칙,
      아트워크 사다리, 로케일 폴백)
- [ ] PLAY-CONSOLE-1.13.md

### 실기기 확인 (2026-08-16, SM-S9xx / Android 15)

확인됨: 탭 5개 · 오늘의 발견(파이썬 대조 결과와 동일한 앨범) · 새로 나온 앨범 ·
반주자 목록 · 앨범 상세(아트워크 · `2026 · 33곡` · 저장/Apple/유튜브뮤직 ·
30초 안내 · 동작 칩 + 박자 + 원곡 길이) · **30초 미리듣기 재생** ·
검색 제안 칩 · 한국어 칩 → 영어 쿼리 검색 · 반주자 상세 · 반주자→앨범 2단 이동 ·
배지 최초 실행 초기화(`musicLastSeenRelease=2026-09-04`).

고친 것:
1. 검색 결과에서 상세를 열면 키보드가 그 위에 남아 있었다 → 오버레이가 열릴 때
   포커스 해제 + IME 숨김
2. 저장 후 버튼 라벨이 `보관함`으로 떴다. 영어는 섹션과 상태가 둘 다 "Saved"라
   한 문자열로 되지만, 한국어는 자리(보관함)와 상태(저장됨)가 다른 말이다 →
   `music_saved_state` 분리

하트 저장 확인: 저장 → 하트 분홍 + 라벨 `저장됨`, 탭으로 돌아가면 `보관함` 섹션
등장, 강제 종료 후에도 유지(`favoriteCatalogAlbums`), 다시 눌러 해제하면 섹션이
사라진다.

배지가 실제로 그려지는 모습은 아직 못 봤다 — 최초 실행에서 0으로 초기화되는 게
정상 동작이라, 카탈로그가 갱신되어야 보인다.
