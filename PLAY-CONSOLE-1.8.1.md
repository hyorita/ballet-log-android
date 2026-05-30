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

## Release Notes (1.8 발표 그대로 — 일반 사용자에겐 1.8.1이 첫 노출)

### 한국어

```
✨ 1.8 업데이트
• Health Connect 자동 기록 — 워치/폰의 발레·필라테스·요가·근력 운동이 끝나면 칼로리·시간·심박수가 담긴 Photo Log가 자동 생성돼요. (24시간 내 앱 열면 동기화)
• 탭으로 사진 추가, 메뉴에서 제거 — 사진만 떼어내도 운동 정보는 그대로 유지됩니다.
• 편집기 자동 저장 — 기존 클래스를 편집하다 닫아도 변경사항이 저장돼요.
• 기본 스텝 이름 복수형 — Pliés, Tendus, Jetés, Fondus, Frappés, Développés.
```

### English

```
✨ What's new in 1.8
• Health Connect auto-logging — Barre, Dance, Pilates, Yoga, and Strength workouts auto-create a Photo Log entry with calories, duration, and heart rate. (Sync by opening the app within 24h.)
• Tap a workout card to attach a photo, remove it from the menu — your workout details stay either way.
• Editor auto-save — existing class log edits save when you close the editor.
• Plural default steps — Pliés, Tendus, Jetés, Fondus, Frappés, Développés.
```

### 日本語

```
✨ 1.8 アップデート
• Health Connect 自動連携 — バレエ・ピラティス・ヨガ・筋トレのワークアウトを終えると、消費カロリー・時間・心拍数が入った Photo Log が自動作成。(24時間以内にアプリを開いて同期)
• タップで写真追加、メニューで取り消し — 写真だけ外してもワークアウト情報は残ります。
• エディタの自動保存 — 既存クラスログは閉じた時点で自動保存。
• デフォルトステップ名を複数形に — Pliés, Tendus, Jetés, Fondus, Frappés, Développés。
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
