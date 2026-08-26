# Ballet Log 1.13 — 업로드 자료

versionCode 14 / versionName 1.13

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.13.apk`
- Play Store: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`

iOS 1.13 동기화. 음악 탭 하나.

---

## 1.13 Release Notes (Play Store / 원스토어 공용)

짧은 버전. 스토어 "이번 버전의 새로운 기능" 필드에 그대로 붙여넣기.
(Play Console 한 언어당 500자 제한 — 아래 셋 다 그 안에 들어감)

### 한국어

```
✨ 1.13
• 음악 탭이 생겼어요 — 발레 클래스 음악은 여기저기 흩어져 있어서, 반주자 이름을 알고 있지 않으면 찾기 어렵습니다. 15개국 반주자 61명, 앨범 780장을 하나씩 골랐어요.
• 오늘의 발견 — 매일 한 장씩 골라 보여드립니다. 자정에 바뀝니다.
• 가기 전에 들어보기 — 모든 곡에 30초 미리듣기가 있고, 플리에·탄듀·아다지오처럼 어떤 동작을 위한 곡인지 함께 표시됩니다. 전체 듣기는 Apple Music이나 유튜브뮤직으로 이어집니다.
• 검색과 보관함 — 이름·학파·작곡가·나라로 찾고, 하트를 누르면 탭 위쪽에 모입니다.
```

### English

```
✨ 1.13
• A new Music tab — Ballet class music is scattered and hard to find unless you know whose name to search for. 61 accompanists from 15 countries, 780 albums, hand-picked.
• Today's discovery — One album a day. It changes at midnight.
• Hear before you go — Every track has a 30-second preview and shows what it's written for: pliés, tendus, adagio. Full tracks play in Apple Music or YouTube Music.
• Search and save — By name, school, composer, or country. Tap the heart to keep an album.
```

### 日本語

```
✨ 1.13
• 新しい音楽タブ — バレエクラスの音楽は散らばっていて、伴奏者の名前を知らないと見つかりません。15か国の伴奏者61名、アルバム780枚を一枚ずつ選びました。
• 今日の発見 — 毎日一枚をお届けします。深夜0時に変わります。
• 行く前に聴く — すべての曲に30秒のプレビューがあり、プリエ・タンデュ・アダージョなど、どの動きのための曲かも表示されます。フル再生は Apple Music か YouTube Music へ。
• 検索と保存 — 名前・流派・作曲家・国から探せます。ハートをタップすると保存されます。
```

---

## 1.13 변경사항 (개발자 메모)

iOS 1.13은 항목 1개(음악 탭). Android는 **반영 + 안드로이드 전용 항목 1개 추가**.

| iOS 항목 | Android 대응 |
|---|---|
| **음악 탭** | ✅ 포팅. 탭 순서는 iOS와 동일하게 맨 뒤(로그·클래스·노트·기록·**음악**). 섹션 순서도 동일: 오늘의 발견 → 새로 나온 앨범 → 보관함(있을 때만) → 반주자. 검색창은 iOS처럼 화면 맨 아래 고정(노트 탭과 같은 방식). 카탈로그는 **iOS와 같은 파일**을 읽는다 (`hyorita.github.io/ballet-log-catalog/v1`) — 캐시 → 번들 시드(`assets/catalog.json`) → 백그라운드 갱신, `catalogVersion`이 더 클 때만 교체. 실패는 조용히(에러 표시 없음). 오늘의 발견은 FNV-1a 날짜 시드라 iOS와 같은 날 같은 앨범이 나온다(단위 테스트로 해시 고정). 정렬은 `rank`(큐레이션 순), `region`은 검색 키로만 쓰고 화면에는 `affiliation`만 표시. 검색은 모든 로케일 텍스트를 훑는다. 탭 배지는 마지막으로 본 발매일 기준, 최초 실행에선 0. |
| — | ➕ **유튜브뮤직 링크** (안드로이드 전용). 앨범·반주자 상세의 Apple Music 링크 옆에 나란히. 국내 안드로이드 사용자 중 Apple Music 비율이 낮아 출구가 하나뿐이면 곤란하다. **Apple Music 링크는 유지** — 아트워크와 미리듣기가 Apple 자산이라 표시 요건 문제가 된다. 새 데이터 없이 `아티스트명 + 앨범명`으로 검색 URL 조립. |

**의존성 추가 없음.** 미리듣기는 `MediaPlayer`(한 번에 한 곡, 큐·백그라운드 재생 없음),
즐겨찾기는 SharedPreferences(앱의 다른 설정과 동일), 카탈로그 fetch는
`HttpURLConnection`(정적 JSON 2종). ExoPlayer·DataStore·OkHttp 전부 이 크기엔 과했다.
자세한 판단 근거는 `PORT-1.13.md`.

**Room 스키마 변경 없음(version 6 유지) → 백업 양방향 안전.**
즐겨찾기는 백업에 포함되지 않는다 — iOS와 같은 한계(카탈로그 id일 뿐이라 DB를 건드릴 값이 아님).

**APK 용량**: 카탈로그 시드 370KB가 assets에 들어간다. 첫 실행이 오프라인이어도
음악 탭이 바로 보이는 값이라 그대로 둔다.

---

## 스토어 심사 시 확인

- [ ] **개인정보처리방침 URL** — Google Play는 URL을 하나만 받는다. 언어 선택이 있는
      페이지를 넣을 것 (iOS·안드로이드 공용 문서)
- [ ] **데이터 세이프티 섹션** — 음악 탭이 추가되며 앱이 네트워크를 쓰게 됐다.
      다만 수집·전송하는 개인정보는 없다(정적 JSON GET + 아트워크 로드뿐).
      기존 신고 내용에 변경이 필요한지 확인
- [ ] Apple 아트워크·미리듣기 표시 요건 현행 확인 (iOS 문서에서도 미해결 상태)

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.13.apk` (versionCode 14)
3. 위 "1.13 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

---

## Play Store 업로드

```bash
./gradlew :app:bundleRelease
# 출력: app/build/outputs/bundle/release/app-release.aab
```

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.

> ⚠️ versionCode 14는 어떤 트랙에든 업로드되는 순간 영구 점유됨. 베타에서 버그 발견 시
> 재사용 불가 → 1.13.1 (versionCode 15)로 bump. (1.8 → 1.8.1 전례 참조)
