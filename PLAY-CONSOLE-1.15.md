# Ballet Log 1.15 — 업로드 자료

versionCode 16 / versionName 1.15

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.15.apk`
- Play Store: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`

iOS 1.15 동기화. Class 탭에 스튜디오/레벨/선생님 태그 — Log 탭이 1.12에서 이미 쓰던 것과 같은
자동완성 풀·필터를 공유.

> ⚠️ **동시 출시 예정.** iOS 1.15는 이 글 작성 시점 기준 아직 App Store 심사 전(로컬 `main`
> 머지만 완료, "1.15 build 1", 그 이후 진행 없음). 아래 "출시 전 확인" 체크리스트를 반드시
> 먼저 볼 것.

---

## 1.15 Release Notes (Play Store / 원스토어 공용)

짧은 버전. 스토어 "이번 버전의 새로운 기능" 필드에 그대로 붙여넣기.
(Play Console 한 언어당 500자 제한 — 아래 셋 다 그 안에 들어감)

### 한국어

```
✨ 1.15
• 클래스에 태그 달기 — Log 탭과 똑같이 클래스 기록에도 스튜디오·레벨·선생님을 붙일 수 있습니다. 한 번 입력해두면 그다음부터 두 탭 어디서든 자동완성으로 뜹니다.
• 태그로 필터링 — Class 탭에서 태그를 탭하면 그 조건에 맞는 기록만 보여줍니다. 여러 개 선택하면 더 좁혀집니다.
```

### English

```
✨ 1.15
• Tag your classes — Add a studio, level, or teacher to any class log, just like the Log tab. Type one once and it shows up as a suggestion on both tabs from then on.
• Filter by tag — Tap a tag on the Class tab to see only the classes that match. Select more than one to narrow it down further.
```

### 日本語

```
✨ 1.15
• クラスにタグを付ける — Log タブと同じように、クラス記録にもスタジオ・レベル・講師を付けられます。一度入力しておけば、次からは両方のタブで自動入力の候補に出てきます。
• タグで絞り込む — Class タブでタグをタップすると、その条件に合う記録だけが表示されます。複数選べばさらに絞り込めます。
```

---

## 1.15 변경사항 (개발자 메모)

iOS 1.15는 항목 1개(Class 탭 태그). Android는 **1:1 포팅**하되, 기존 Log 탭(1.12)이
이미 갖고 있던 인프라(공유 태그 풀, 바텀시트, 필터 행)를 그대로 재사용 — 새 위젯을
만들지 않음.

| iOS 항목 | Android 대응 |
|---|---|
| **Class 탭 태그** (스튜디오/레벨/선생님) | ✅ 포팅. `TagInputSheet`/`SuggestionChip`/`TagFilterRow`를 Log 탭 전용 파일에서 `ui/common/`으로 추출해 두 탭이 공유. 자동완성 풀도 같은 Room 테이블(`photo_log_tags`) — 한쪽 탭에서 입력한 태그가 다른 탭에서도 바로 제안으로 뜸. 필터는 Log 탭과 동일한 flat AND(카테고리 구분 없이 선택한 태그 전부를 포함해야 함). |

**iOS 버그 하나는 의도적으로 안 가져옴.** iOS `EditorView`는 재편집 시 배열 인덱스로
필드를 채우는데, 저장할 때는 빈 값을 걸러내고 저장한다 — 그래서 스튜디오를 비우고
선생님만 채우면, 재편집할 때 그 값이 스튜디오 칸에 잘못 들어간다. Android는 Log 탭이
이미 쓰던 방식대로 3칸을 그대로(빈 값 포함) 저장해서 이 문제를 원천적으로 피함.
실기기에서 정확히 이 케이스(스튜디오·레벨 비우고 선생님만 채움)로 검증 완료.

**UI 방식도 다름, 의도적.** iOS는 캡슐 안에서 바로 타이핑 + 포커스에 따라 제안이
바뀌는 방식이지만, Android는 Log 탭이 이미 검증해서 쓰고 있는 바텀시트 방식을 그대로
재사용 (캡슐은 탭해서 시트를 여는 트리거). 같은 문제를 다른 방식으로 두 번 만들지
않기 위한 선택.

**당일 후속 수정**: 제안 칩을 탭하면 필드를 채우는 동시에 시트가 바로 닫히도록 수정
(원래는 체크마크를 따로 눌러야 했음). 공유 컴포넌트라 Log 탭에도 동일하게 적용됨.

**Room 스키마 변경**: `class_logs`에 `tagsJson` 컬럼 추가, `MIGRATION_6_7`
(version 6 → 7) — 기존 `MIGRATION_3_4`/`5_6`와 동일하게 `ALTER TABLE ADD COLUMN
... DEFAULT`만 쓰는 비파괴적 마이그레이션. `exportSchema = false`라 Room 자체
스키마 검증이 없어서, 기존 DB 위에서 마이그레이션이 실제로 깨지지 않는지 에뮬레이터
+ 실기기(진짜 사용 데이터 있는 기기) 양쪽에서 직접 열어서 확인함.

**새 의존성 없음.**

---

## 출시 전 확인 (동시 출시라서 특히 중요)

- [ ] **iOS 1.15가 App Store 심사를 통과했는지** — 통과 전엔 Android도 올리지 않는다(동시 출시 합의)
- [ ] 개인정보처리방침 URL — 변경 없음
- [ ] 데이터 세이프티 섹션 — 변경 없음(이번 버전은 추가 권한/수집 없음, 기존 Room 데이터 범위 안)

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.15.apk` (versionCode 16)
3. 위 "1.15 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

---

## Play Store 업로드

```bash
./gradlew :app:bundleRelease
# 출력: app/build/outputs/bundle/release/app-release.aab
```

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.

> ⚠️ AAB는 **버전을 올린 뒤 반드시 다시 빌드**할 것 — 1.14 때 옛날 AAB(versionCode 14)를
> 그대로 올렸다가 "버전 코드가 이미 사용되었습니다" 오류를 본 적 있음. 이번엔
> `assembleRelease`와 `bundleRelease`를 같은 시점에 같이 돌려서 APK·AAB 버전이 어긋나지
> 않게 함.
>
> versionCode 16은 어떤 트랙에든 업로드되는 순간 영구 점유됨. 베타에서 버그 발견 시
> 재사용 불가 → 1.15.1 (versionCode 17)로 bump. (1.8 → 1.8.1 전례 참조)
