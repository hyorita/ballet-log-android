# Ballet Log 1.8.1 — Production 업로드 자료

versionCode 9 / versionName 1.8.1

> versionCode bump 이력: 8 → 9 (검토 통과한 1.8 (versionCode 8) 베타 빌드의
> 칩 키워드 fix 적용. 베타 트랙에 publish된 versionCode 8은 재사용 불가라
> 9로 bump 후 새 릴리스로 등록).

빌드 산출물:
- Play Store (Production track): `app/build/outputs/bundle/release/app-release.aab`
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.8.1.apk`

---

## 1.8.1 변경사항 (개발자 메모)

1.8 전체 기능 + iOS에서 발견·수정된 칩 키워드 매칭 버그 fix.

| 항목 | 변경 |
|---|---|
| `BalletTerms.kt` | 칩 셋을 named constant로 분리, en/ja/ko 키워드 변형 추가 (ex. "탄듀", "タンデュ" → tenduChips). 우선순위 보존 ("allegro 3" before "jeté", "grand battement" before "battement"/"grand", "à terre"/"en l'air" before per-step). jeté/fondu/frappé chip set이 자기 자신 용어로 시작 (이전엔 "tendu" 첫). adagio에서 "temps lié" 제거, à terre는 "rond, grand, plié..."로 정리, grand battement에 "grand, battement" 추가. |
| `build.gradle.kts` | versionCode 9, versionName 1.8.1 |

1.8 기능 변경분은 `PLAY-CONSOLE-1.8.md` 참조.

---

## Release Notes — 1.8.1 패치

1.8을 먼저 프로덕션 게시한 뒤 곧바로 1.8.1을 올리는 흐름이라
1.8.1 노트는 패치 fix만 짧게 다룸. 1.8 기능 변경 사용자 설명은
1.8 게시 때 이미 전달됨 (`PLAY-CONSOLE-1.8.md` 참조).

### 한국어

```
🛠 1.8.1
• 콤비네이션 메모 칩 바 — 한국어/일본어로 입력된 step 이름에서도 그 step에 맞는 발레 용어 칩이 먼저 정렬돼요.
• Jetés / Fondus / Frappés 칩이 각자 본인 용어부터 시작하도록 정리.
```

### English

```
🛠 1.8.1
• Combination notes chip bar — Korean and Japanese step names now reorder the chips to terms relevant to that step, matching the English behavior.
• Jetés, Fondus, and Frappés chip sets now lead with their own step term.
```

### 日本語

```
🛠 1.8.1
• コンビネーションノートのチップバー — 韓国語・日本語のステップ名でも、そのステップに合うバレエ用語チップが先頭に並びます。
• Jetés / Fondus / Frappés のチップがそれぞれの用語から始まるように整理。
```

---

## Play Console 업로드 절차

1. **현재 8 (1.8) 드래프트 폐기** — 게시 개요 → "변경사항이 아직 검토를 위해 전송되지 않음" → 8 (1.8) 행의 ⋮ → 폐기
2. Production → **새 버전 만들기** → `app-release.aab` (versionCode 9) 업로드
3. Release notes 붙여넣기 (위 한국어 블록)
4. 저장 → 검토 → 게시

심사 다시 받음 (구버전 1.8과 별개 리뷰).

---

## 원스토어 업로드

1. 원스토어 개발자 센터 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.8.1.apk` (versionCode 9)
3. 한국어 release notes 붙여넣기
4. 심사 제출
