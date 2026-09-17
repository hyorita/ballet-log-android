# Ballet Log 1.14 — 업로드 자료

versionCode 15 / versionName 1.14

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.14.apk`
- Play Store: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`

iOS 1.14 동기화. 음악 탭 2차 — 보관함 그리드, 신보 배지 버그 수정, 오늘의 발견 계절 게이트, 포그라운드 새로고침.

> ⚠️ **동시 출시 예정.** iOS 1.14는 이 글 작성 시점 기준 아직 App Store 심사 전(로컬 `main`
> 머지만 완료, TestFlight 업로드 대기). 아래 "출시 전 확인" 체크리스트를 반드시 먼저 볼 것 —
> 특히 카탈로그 v10(`addedIn`/`excludeFromDiscovery` 필드) 공개 배포 여부.

---

## 1.14 Release Notes (Play Store / 원스토어 공용)

짧은 버전. 스토어 "이번 버전의 새로운 기능" 필드에 그대로 붙여넣기.
(Play Console 한 언어당 500자 제한 — 아래 셋 다 그 안에 들어감)

### 한국어

```
✨ 1.14
• 보관함 정리 — 음악 탭 보관함에 담은 앨범 개수가 표시되고, 일정 수를 넘으면 "전체 보기"로 전체 그리드를 열 수 있습니다. 앨범을 길게 누르면 바로 삭제할 수 있어요.
• 신보 배지 수정 — 발매일이 미래로 찍힌 앨범 때문에 배지가 0에서 멈춰버리던 버그를 고쳤습니다.
• 오늘의 발견, 제철에 맞게 — 크리스마스·할로윈 앨범이 이제 해당 시즌에만 등장합니다.
• 항상 최신 — 앱으로 돌아올 때마다 음악 탭이 새로고침됩니다.
```

### English

```
✨ 1.14
• Saved, organized — Your saved albums on the Music tab now show a count, and once you've saved more than a handful, "Show all" opens a full grid. Press and hold an album to remove it.
• New release badge, fixed — Fixed a bug where the badge could get stuck at zero if an album was dated ahead of its actual publish.
• Today's discovery, in season — Holiday and Halloween albums now only turn up around the holidays and Halloween, not any random day of the year.
• Always current — The Music tab refreshes when you come back to the app, not just on a cold start.
```

### 日本語

```
✨ 1.14
• 保存済みを整理 — 音楽タブの保存済みに枚数が表示され、一定数を超えると「すべて表示」でグリッド全体を開けます。長押しで削除できます。
• 新着バッジの修正 — 発売日が未来のアルバムがあるとバッジが0で止まってしまう不具合を修正しました。
• 今日の発見、季節に合わせて — クリスマス・ハロウィンのアルバムは、それぞれの季節にだけ登場するようになりました。
• 常に最新 — アプリに戻るたびに音楽タブが更新されます。
```

---

## 1.14 변경사항 (개발자 메모)

iOS 1.14는 항목 4개, 전부 Music 탭. Android는 **1:1 포팅**, 안드로이드 전용 추가 없음.

| iOS 항목 | Android 대응 |
|---|---|
| **보관함 그리드** (개수 + 전체 보기 + 길게 눌러 삭제) | ✅ 포팅. rail은 기존처럼 최대 8개(`SAVED_RAIL_LIMIT`), 8개 초과 시 "전체 보기" → 2열 `LazyVerticalGrid`(`SavedAlbumsScreen.kt`, 신규 파일). 삭제는 iOS의 컨텍스트 메뉴 대신 앱 기존 관례(Notes/PhotoLog와 동일한 long-press → `AlertDialog` 확인) 사용 — 플랫폼 관용구 차이, 기능은 동일. |
| **신보 배지 수정** (날짜 → `catalogVersion` 기반) | ✅ 포팅. `MusicPreferences`가 `musicLastSeenRelease`(String) 대신 `musicLastSeenVersion`(Int) 저장. 배지 = `addedIn > lastSeenVersion`인 앨범 수. 기존 date 기반 pref 키는 그냥 버려둠(마이그레이션 불필요 — 새 Int 기본값 0이 "최초 실행" 가드와 동일하게 동작, iOS도 같은 판단). |
| **오늘의 발견 계절 게이트** | ✅ 포팅. `themes`에 `christmas`(11·12월만) / `halloween`(10월만) 태그가 있으면 그 달에만 후보 풀에 포함. `excludeFromDiscovery` 수동 제외 필드도 추가. 요일 해시(UTC)와 계절 판정(기기 로컬 달력)이 기준이 다른 것은 iOS 원본 그대로 유지 — 의도된 불일치, "고치지" 않음. |
| **포그라운드 새로고침** | ✅ 포팅. 새 의존성 없이 기존 `MainActivity.onStart()`(1.8 Health Connect 자동 가져오기용으로 이미 있던 foreground 훅)에 `CatalogRepository.load()` 호출 추가. 실제 네트워크 fetch는 저장소 안에서 1시간 스로틀(iOS와 동일). |

**공유 카탈로그 스키마 변경**: `CatalogAlbum`에 `addedIn: Int?`, `excludeFromDiscovery: Boolean?` 추가(둘 다 nullable, 없으면 무시). iOS와 같은 파일(`hyorita.github.io/ballet-log-catalog/v1`)을 읽으므로 Android 쪽에서 스키마 문서를 따로 만들지 않음 — iOS `catalog/CATALOG-SCHEMA.md` 참조.

> ⚠️ **카탈로그 아직 미배포.** 이 글 작성 시점 기준 공개 카탈로그는 `addedIn` 필드가 없는
> 구버전이다. Android가 지금 이 버전을 받으면 `addedIn`이 전부 `null`로 처리돼 신보 배지는
> 항상 0으로 보인다 — 버그 아님, iOS 1.14도 동일 상태. 카탈로그 v10 공개 배포 후 정상 동작.

**Room 스키마 변경 없음** → 백업 양방향 안전. 즐겨찾기는 여전히 백업 미포함(카탈로그 id만 저장).

**새 의존성 없음.** 그리드는 기존 `androidx.compose.foundation` 안에 있는 `LazyVerticalGrid`(첫 사용, 별도 라이브러리 불필요). long-press는 기존 `combinedClickable` 관용구 재사용(Notes/PhotoLog 탭과 동일 패턴).

---

## 출시 전 확인 (동시 출시라서 특히 중요)

- [ ] **iOS 1.14가 App Store 심사를 통과했는지** — 통과 전엔 Android도 올리지 않는다(동시 출시 합의)
- [ ] **카탈로그 v10 공개 배포 여부** (`addedIn`/`excludeFromDiscovery` 포함) — 이 저장소는
      Apple 심사 통과 후 배포하기로 iOS 쪽에서 결정됨. 배포 전 출시하면 신보 배지가 계속 0으로
      보이는 상태로 시작하게 됨(기능은 정상, 데이터만 아직 없음)
- [ ] 개인정보처리방침 URL — 변경 없음(1.13과 동일 공용 문서)
- [ ] 데이터 세이프티 섹션 — 변경 없음(1.13에서 이미 반영됨, 이번 버전은 추가 권한/수집 없음)

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.14.apk` (versionCode 15)
3. 위 "1.14 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

---

## Play Store 업로드

```bash
./gradlew :app:bundleRelease
# 출력: app/build/outputs/bundle/release/app-release.aab
```

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.

> ⚠️ versionCode 15는 어떤 트랙에든 업로드되는 순간 영구 점유됨. 베타에서 버그 발견 시
> 재사용 불가 → 1.14.1 (versionCode 16)로 bump. (1.8 → 1.8.1 전례 참조)
