# Ballet Log 1.7 — 업로드 자료

versionCode 7 / versionName 1.7

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.7.apk` (5.6 MB)
- Play Store: 필요 시 `./gradlew :app:bundleRelease` 로 AAB 생성

---

## 원스토어 1.7 Release Notes

### 한국어

```
✨ 1.7 업데이트
• 발레 용어 칩 바 — 콤비네이션 메모 작성 시 키보드 위에 자주 쓰는 발레 용어를 표시
• 컨텍스트 추천 — 동작 이름에 따라 관련 용어가 먼저 정렬됩니다 (예: Adagio → croisé, temps lié)
• 자동 완성 — 한글이나 영문 첫 글자만 입력해도 후보가 필터링됩니다
• 용어 언어 선택 — 설정에서 칩에 표시할 언어를 시스템/English/한국어 중 선택
• 자동 저장 — 기존 클래스를 편집한 뒤 화면을 닫으면 변경 내용이 자동으로 저장됩니다
• 입력 시 자동 스크롤 — 콤비네이션 메모 입력 칸을 탭하면 해당 칸이 화면 상단으로 이동
```

---

## 1.7 변경사항 (개발자 메모)

iOS 1.7 기능 동기화.

| 항목 | 변경 |
|---|---|
| `BalletTerms.kt` (신규) | 94개 기본 발레 용어 + 동작 이름→추천 용어 매핑 + SharedPreferences 기반 사용 빈도 통계 |
| `BalletTerm` + `TermLanguagePreferences` | 영/한 표기, 시스템 언어 fallback, `balletTermUsage.v1` 키로 iOS 호환 |
| `TermChipBar.kt` (신규) | 키보드 위 LazyRow chip bar — 컨텍스트/사용 빈도/diacritic-insensitive prefix 매칭으로 정렬 |
| `TermFieldHelpers.kt` (신규) | `wordFragmentBeforeCaret`, `insertTermAtWordBoundary` |
| `EditorScreen.kt` | `TextFieldValue` 전환 (caret 기반 word replacement), step.note focus → chip bar context 갱신 / `LazyListState.animateScrollToItem` 으로 포커스 칸을 화면 상단 정렬, `TopAppBarDefaults.enterAlwaysScrollBehavior` 로 헤더 collapse, `DisposableEffect` 자동 저장 (iOS `persistInPlace` parity) |
| `SettingsScreen.kt` | Combination terms 언어 선택 섹션 추가 (`SingleChoiceSegmentedButtonRow`) |
| `strings.xml` (en/ko/ja) | `settings_combo_terms`, `term_language_*` 신규 |
| `MainActivity.kt` | `LocalBottomBarVisible` 추가 — overlay 활성 시 root NavBar 숨김 + bottom systemBars 인셋 제거 |
| `*Screen.kt` (Photo/Class/Home/Notes/History) | `Dialog(usePlatformDefaultWidth=false)` → inline Surface overlay 전환. Compose Dialog 의 height WRAP_CONTENT 이슈로 LazyColumn 스크롤 실패 / BottomCenter overlay 화면 밖 위치 문제 해소 |
| `build.gradle.kts` | versionCode 7, versionName 1.7, APK 출력 이름 동적 (`BalletLog-$versionName.apk`) |
| 신규 용어 | `passé par terre` / 파세파테르, `temps lié` / 탕리에 |

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.7.apk` (versionCode 7)
3. 위 "원스토어 1.7 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

---

## Play Store 업로드 (필요 시)

```bash
./gradlew :app:bundleRelease
# 출력: app/build/outputs/bundle/release/app-release.aab
```

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.

### Play Store용 한국어 Release Notes (원스토어와 동일 사용 가능)

위 "원스토어 1.7 Release Notes" 텍스트 그대로.

### English

```
✨ What's new in 1.7
• Ballet term chip bar — frequently-used ballet terms appear above the keyboard while editing combinations
• Context-aware suggestions — terms relevant to the step name surface first (e.g. Adagio → croisé, temps lié)
• Quick filter — type the first letter (Hangul or Latin) and the chip list narrows to matching terms
• Term language picker — choose System / English / 한국어 in Settings
• Auto-save — closing an existing class persists your edits automatically
• Scroll on focus — tapping a combination note brings that field to the top of the screen
```

### 日本語

```
✨ 1.7 アップデート
• バレエ用語チップバー — コンビネーションメモ入力中、キーボード上によく使う用語を表示
• 文脈に応じたおすすめ — ステップ名に合う用語が先頭に並びます（例: Adagio → croisé、temps lié）
• 入力補完 — 一文字（ハングル / アルファベット）入力で候補が絞り込まれます
• 用語の表示言語 — 設定でシステム / English / 한국어 を選択
• 自動保存 — 既存クラスを編集して画面を閉じると自動で保存されます
• フォーカス時の自動スクロール — コンビネーションメモをタップすると、その欄が画面上部に移動
```

---

## 출시 후 메모

- **원스토어 기존 사용자**: 1.6.1 → 1.7 자동 업데이트. 콤비네이션 메모 칸에서 키보드 위 칩 바 신규 노출. 설정 → "콤비네이션 용어" 에서 표시 언어 변경 가능.
- **신규 기능 동선 확인**: 클래스 추가 → Barre/Center 탭에서 step note 탭 → 키보드 + 칩 바가 함께 표시되는지. 칩 탭 시 현재 단어 자리에 용어가 삽입되고 한 칸 띄움.
- **칩 언어 우선 순위**: 설정값 (System/English/Korean) → System 선택 시 디바이스 언어가 한국어면 한국어 표기, 그 외엔 영어.
