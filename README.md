# 無心トランプ – README

最終更新: 2026-02-12 (JST)


## 1. アプリ概要

**無心トランプ**は、  
トランプゲームに没頭できる Android アプリです。  
搭載ゲームは以下の 6 種：
- Up & Down（ハイ＆ロー）
- ババ抜き（Old Maid）
- 大富豪（Daifugo）
- ブラックジャック（Blackjack）
- ポーカー（Jacks or Better）


## 2. タイトル画面（スクリーンショット）

アプリ起動時には、Hero画像（`index_picture.jpg`）と アプリタイトル「無心トランプ」が表示されます。
> ./app/src/main/res/drawable/index_picture.jpg
> 
> ## 3. 画面構成と遷移

ゲーム選択（Menu）  
├─ Up & Down  
├─ ババ抜き  
├─ 大富豪  
├─ ブラックジャック  
└─ ポーカー  
　　↳（ゲーム終了時）リザルト

- すべてのゲームで **「ゲーム選択 → 開始 → ゲーム → リザルト」** の流れに統一
- UI は **縦長レイアウト**を基本とする
- トップ画面にはタイトル画像とアプリ名を重ねて表示


## 4. セットアップ方法

1. Android Studio (Koala 以降) をインストール
2. 本プロジェクトを「Open」
3. Run ▶（エミュレータ / 実機）
※ index_picture.jpg が無い場合はビルド時に警告が出ます。

## 5. 各ゲームの遊び方（簡易）

### 5.1 Up & Down（ハイ＆ロー）
- 左カード（表）と右カード（裏）の大小を予想
- Up / Down を押して判定
- **A 最強 → 2 最弱**
- 正解でスコア ×2、不正解で 0 & 終了

### 5.2 ババ抜き（Old Maid）
- 左隣の裏カードをタップして引く
- 取った直後にペアを自動削除
- 最後に残った人（JOKER保持者）が敗者

### 5.3 大富豪（Daifugo）
- シングル / グループ / 階段 を選んで「出す」
- **革命（4枚）・縛り（同スート2連）・8切り** fully対応
- 場のカードは「大」「扇形」「横並び」で直感的表示

### 5.4 ブラックジャック（Blackjack）
- Hit / Stand
- A は 11 または 1
- ディーラーは **S17（ソフト17でスタンド）**
- BJ = 1.5倍

### 5.5 ポーカー（Jacks or Better）
- 5枚配布 → HOLD を選択 → 引き直し
- 結果で「Jacks or Better 〜 Royal Flush」まで判定
- クレジット制


## 6. ファイル構

- 全ファイルを **同一階層に置く構成**
- カード UI だけ `PlayingCards.kt` に共通化


## 7. 開発環境

- Android Studio Koala
- Kotlin 1.9.25
- Compose Compiler 1.5.15
- Material3
- StateFlow + ViewModel
- 端末：Android 7.0（API 24）以降 


## 8. 既知の注意点

- AnimatedContent を使う箇所は **@OptIn(ExperimentalAnimationApi::class)** が必要
- `index_picture.jpg` は **drawable フォルダに配置**


## 9. 今後の拡張（ToDo）

- 大富豪：都落ち、2上がり禁止、JOKERワイルド階段
- BJ：Split、Double、Bet 変更、シャッフル演出
- ポーカー：BET×1〜5、配当表画面
- ゲーム履歴、勝率集計
- オンライン対戦  