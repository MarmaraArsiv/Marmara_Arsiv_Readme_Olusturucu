# 🛠 Marmara Bilgisayar README Oluşturucu

[Marmara Bilgisayar Mühendisliği Arşivi](https://github.com/MarmaraArsiv/tek_bilgisayar)'nin bütün README'lerini
(ana sayfa, 8 dönem, seçmeli havuzları ve her dersin kendi sayfası) JSON verilerinden üreten masaüstü aracı.
Ders, hoca ya da dönem bilgisi değiştiğinde README'leri elle düzenlemek yerine veriyi arayüzden girip aracı
çalıştırmak yeterli.

## Gereksinimler

- **JDK 21** — Ubuntu'da `sudo apt install openjdk-21-jdk`. Maven kurmaya gerek yok, `mvnw` kendisi indirir.
- Arşiv reposunun aracın yanında olması: `../Marmara_Bilgisayar_Muhendisligi_Arsiv`. Başka bir yerdeyse
  araç ilk açılışta klasörü sorar.

## Arayüz

```bash
./arayuz.sh          # Windows: arayuz.bat
```

Önerilen sıra:

1. **Git İşlemleri › ⬇️ GitHub'dan Çek** — GitHub'daki son hâli al (botlar README'leri sık günceller).
2. **Ders / Hoca / Dönem / Giriş** pencerelerinden düzenle. Değişiklikler JSON dosyalarına kaydedilir.
3. **Git İşlemleri › 📝 Readme Güncelle** — README'leri JSON'dan üretir, yalnızca değişenleri yazar.
4. **⬆️ Değişiklikleri GitHub'a Pushla** — değişen dosyaları ve farklarını gör, commit mesajını yaz.

## Komut satırı

```bash
./readme_olustur.sh                     # README'leri JSON'dan üretir
./readme_olustur.sh --kontrol           # hiçbir şey yazmaz; README'ler JSON ile uyumlu mu bakar
./readme_olustur.sh <arşiv> --kontrol   # arşiv başka bir klasördeyse
./build.sh                              # yalnızca derler: target/marmara-readme-araci-0.1.0.jar
```

`--kontrol` uyumsuz README varsa listeleyip 1 koduyla çıkar; JSON'da hata varsa 2 koduyla.

## Veriler

Arşivde `.github/json_dosyalari/` altında:

| Dosya | İçerik |
|---|---|
| `dersler.json` | Her ders: klasörü (ör. `2-2/Elektronik Devrelere Giriş`), tipi, müfredatı ve notu, yıl yıl dersi yürüten hocalar, derse dair öneriler. Listedeki sıra README'deki sıradır. |
| `hocalar.json` | Her hoca: kısaltma, ana README'deki grubu, ofis, AVESİS linki, cinsiyet (kart emojisi), verdiği dersler. Grupların sırası ve açıklamaları. |
| `donemler.json` | 8 dönem (başlık, genel tavsiyeler) ve seçmeli havuzları (başlık, açıklama). |
| `giris.json` | Ana sayfanın başı: başlık, açıklama, geri bildirim paragrafı, Katkıda Bulunun bölümü, sayaç, içindekiler. |
| `repo_kullanimi.json` | Repo Kullanımı ve Kavramlar bölümleri. |
| `katkida_bulunanlar.json` | Katkıda bulunanlar (GitHub'daki betik pull request'lerden günceller). |
| `konfigurasyon.json` | Repo adresi, Google Form linkleri ve soru kimlikleri, README'lerin yeri (`dokumanlar_repo_yolu`). |

## Nasıl çalışır

- Her README JSON'dan baştan üretilir. Veri değişmediyse çıktı mevcut README ile **birebir aynıdır**; bunu
  gerçek arşive karşı çalışan test doğrular.
- GitHub'daki betiklerin doldurduğu bloklar mevcut README'den aynen korunur: ⭐ yıldızlar, 💬 onaylı yorumlar,
  hoca yorumları ve 📊 doluluk tablosu. Yeni ders ya da hoca için bu blokların boş hâli yazılır; GitHub ilk
  push'ta doldurur.
- Kurallar:
  - Hocalar grup içinde, kısaltmalar ise listede Türkçe alfabeye göre dizilir.
  - Hoca kartındaki dersler en son verildiği yıla göre sıralanır.
  - Ana sayfadaki ders kartında yalnızca en güncel yılın hocaları görünür.
  - "2026 Öncesi" müfredatındaki bir ders 2026-2027 ve sonrasında açıldıysa yılın yanına
    "bu yıl yeni girenler bu dersi alamaz" açıklaması eklenir.
- Üretimden önce JSON doğrulanır: boş ya da tekrarlanan klasör, yanlış yıl biçimi, çakışan kısaltma veya
  tanımsız grup gibi hatalarda **hiçbir dosyaya dokunulmaz**, hataların listesi gösterilir.
- JSON dosyaları Python'un `json.dumps(indent=4)` biçiminde yazılır ve araç tanımadığı alanları silmez;
  böylece arayüzden kaydetmek GitHub betikleriyle çakışmaz.

## Testler

```bash
./mvnw test -Darsiv.json=../Marmara_Bilgisayar_Muhendisligi_Arsiv
```

Denetlenenler:

- Arşivdeki README'ler JSON'dan birebir üretiliyor; JSON'lar okunup yazılınca değişmiyor.
- Yeni bir ders eklenince ders sayfası, havuz listesi, ana sayfa kartı ve hoca kartı doğru oluşuyor.
- Bütün pencereler ve düzenleme diyalogları ekransız (Monocle) açılıp hatasız kapanıyor.
- Arayüz bir kullanıcı gibi kullanılıyor (alan doldurma, düğmeye basma, onay kutuları):
  - ders ekleme (yıl, listeden hoca, not, listede olmayan isim), düzenleme, silme ve Ctrl+Z ile geri alma;
  - hoca ekleme (kısaltma ve grup önerisi) ve terfi: ünvan değişince grup, derslerdeki ad ve onaylı
    yorumlar yeni ada taşınıyor;
  - Readme Güncelle yalnızca değişen dosyaları yazıyor, JSON hatalıysa hiçbir dosyaya dokunmuyor;
  - push: yerel bir test reposunda boşluklu/Türkçe adlı dosyalar seçilip commit'leniyor, botun arada
    attığı commit'in üstüne gönderiliyor, işaretlenmeyen değişiklik yerelde kalıyor.
- Link ve sıralama yardımcıları doğru çalışıyor.

`-Darsiv.json` verilmezse arşive bağlı testler atlanır. Testler arşive hiçbir şey yazmaz; hepsi geçici
kopyalarla çalışır, push testi de GitHub'a değil geçici bir yerel repoya gönderir.
