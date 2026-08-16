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

## 옮길 파일

| iOS | 안드로이드 (제안) |
|---|---|
| `Models/MusicCatalog.swift` (161줄) | `data/model/MusicCatalog.kt` — Gson 데이터 클래스 |
| `Services/CatalogService.swift` (162줄) | `data/catalog/CatalogRepository.kt` |
| `Services/AlbumFavorites.swift` (35줄) | `data/catalog/AlbumFavorites.kt` — DataStore |
| `Services/PreviewPlayer.swift` (62줄) | `ui/music/PreviewPlayer.kt` — Media3 ExoPlayer |
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

## 추가할 의존성

현재 `app/build.gradle.kts`에 없는 것:

```kotlin
implementation("androidx.media3:media3-exoplayer:1.4.1")   // 30초 미리듣기 (AAC/MP4)
implementation("androidx.datastore:datastore-preferences:1.1.1")  // 즐겨찾기
implementation("com.squareup.okhttp3:okhttp:4.12.0")       // 카탈로그 fetch
```

Coil · Gson · Navigation은 이미 있다. OkHttp 대신 `HttpURLConnection`으로
가도 되는 규모(정적 JSON 2종)라 착수 시 판단.

## 안드로이드 전용 결정

**유튜브뮤직 검색 링크를 함께 둔다.** 국내 안드로이드 사용자 중 Apple Music
비율이 낮다. 다만 **Apple Music 링크를 빼면 안 된다** — 아트워크와 미리듣기가
Apple 자산이라 표시 요건 문제가 된다. 둘 다 노출. 새 데이터 없이 URL 조립:

```kotlin
val q = Uri.encode("${artist.name} ${album.title}")
"https://music.youtube.com/search?q=$q"
```

**탭 배치.** 현재 4탭(Log · Class · Notes · History)에 Music이 붙어 5탭이 된다.
iOS가 Music을 맨 끝에 둔 건 기존 사용자의 탭 위치를 안 흔들려는 이유라
안드로이드에는 해당 없지만, **개인 기록 탭 무리(Log·Class·Notes·History)를
가르지 말 것**이라는 제약은 그대로 유효하다. 즉 맨 앞 또는 맨 뒤.

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
- [ ] 의존성 추가
- [ ] 모델 · 리포지토리 · 즐겨찾기
- [ ] MusicScreen · ArtistDetail · AlbumDetail
- [ ] 미리듣기 플레이어
- [ ] 탭 추가 (5탭)
- [ ] EN/KO/JA 문자열
- [ ] PLAY-CONSOLE-1.13.md
