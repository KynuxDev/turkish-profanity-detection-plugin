# Turkish Profanity Detection - Türkçe Küfür Tespit Eklentisi

![TurkishProfanityDetection Logo](https://i.imgur.com/placeholder-logo.png)

## Kategori
**Chat**

## Başlık
**AntiSwear - Türkçe Küfür ve Hakaret Tespit Eklentisi**

## Etiketler
`türkçe küfür tespit`, `yapay zeka filtreleme`, `küfür engelleme`, `chat koruma`, `moderasyon aracı`, `profanity filter`, `discord entegrasyonu`, `veritabanı desteği`, `custom komutlar`

## Minecraft Sürüm Uyumluluğu
- 1.8
- 1.9
- 1.10
- 1.11
- 1.12
- 1.13
- 1.14
- 1.15
- 1.16
- 1.17
- 1.18
- 1.19
- 1.20+

## 🌟 Eklenti Açıklaması

**Turkish Profanity Detection** (Türkçe Küfür Tespit Eklentisi), sunucunuz için geliştirilmiş, yapay zeka destekli, kapsamlı bir Türkçe küfür ve hakaret tespit sistemidir. Özellikle Türk sunucuları için optimize edilmiş bu eklenti, sohbet kanallarınızı temiz ve güvenli tutarken moderasyon ekibinizin işini önemli ölçüde kolaylaştırır.

Bu eklenti, gelişmiş yapay zeka algoritmalarını kullanarak basit kelime filtrelerinin tespit edemediği gizli küfürleri ve hareketleri algılar. Yapay zeka modeli, özellikle Türkçe dil yapısına göre eğitilmiştir ve sürekli olarak güncellenmektedir.

## 🚀 Temel Özellikler

- ✅ **Yapay Zeka Destekli Analiz**: GPT-4.5 tabanlı yapay zeka desteği ile kelime listelerini aşan, bağlam temelli tespit sistemi
- ✅ **Şiddet Seviyelerine Göre İşlemler**: Basit argo sözcüklerden ağır küfürlere kadar 5 farklı şiddet seviyesine göre otomatik işlemler
- ✅ **Gerçek Zamanlı Filtreleme**: Mesajlar gönderilmeden önce analiz edilir, uygunsuz içerikler anında engellenir
- ✅ **Discord Entegrasyonu**: Tespit edilen ihlallerin Discord kanalınıza otomatik olarak raporlanması
- ✅ **Kullanıcı Dostu Yönetim Paneli**: Sezgisel bir arayüz ile tüm istatistikleri ve ayarları yönetin
- ✅ **Veritabanı Desteği**: MySQL/SQLite desteği ile istatistikleri kalıcı olarak saklayın
- ✅ **PlaceholderAPI Entegrasyonu**: Küfür istatistiklerini diğer eklentilerinizde kullanın
- ✅ **Anti-Spam Koruması**: Hızlı mesaj göndermeye karşı koruma sistemi
- ✅ **Özelleştirilebilir Komutlar**: Tespit durumlarında çalıştırılacak özel komutları ayarlayın

## 🛡️ Sunucunuzu Nasıl Korur?

Plugin, oyuncularının mesajlarını hem klasik küfür kelime listeleri hem de modern yapay zeka algoritmaları ile analiz eder. Bu sayede:

- **Kötü niyetli kaçamakları** (karakter değiştirme, boşluk ekleme vb.) tespit eder
- **Bağlam temelli analizler** yapar, yanlış alarmları minimuma indirir
- **Şiddet seviyesine göre** farklı yaptırımlar uygular (uyarı, mute, ban, vb.)
- **Tüm ihlal kayıtlarını** detaylı şekilde arşivler ve raporlar

## ⚙️ Kolay Kurulum ve Yapılandırma

Kurulum son derece basittir:
1. Eklentiyi sunucunuzun plugins klasörüne yükleyin
2. Sunucuyu yeniden başlatın
3. `config.yml` dosyasından tüm özellikleri özelleştirin
4. İsteğe bağlı olarak MySQL/Discord yapılandırmasını tamamlayın

## 📊 Kapsamlı İstatistikler

- Oyuncu bazlı küfür istatistikleri
- En sık kullanılan uygunsuz kelimeler
- Şiddet seviyesi dağılımı
- Zaman bazlı istatistik raporları

## 🔧 Teknik Detaylar

### Komutlar
- `/tpd help` - Komut yardımını gösterir
- `/tpd reload` - Eklentiyi yeniden yükler
- `/tpd version` - Versiyon bilgisini gösterir
- `/tpd stats <oyuncu>` - Oyuncu istatistiklerini gösterir

### İzinler
- `turkishprofanitydetection.bypass` - Filtreyi atlamak için
- `turkishprofanitydetection.commands` - Komutları kullanmak için
- `turkishprofanitydetection.admin` - Yönetici bildirimlerini almak için
- `turkishprofanitydetection.statistics` - İstatistikleri görüntülemek için

### API Kullanımı
Eklenti, bulut tabanlı bir API kullanarak yapay zeka analizlerini gerçekleştirir:
- Yüksek performanslı Minecraft endpoint: `http://api.kynux.cloud/api/swear/minecraft-check`
- Yapay zeka tarafından önerilen aksiyon (warn, mute, kick, ban)
- Güven skoru ve tespit edilen içeriğin ciddiyeti
- Minecraft uyumluluğu kontrolü

### Veritabanı Yapısı
- Oyuncu bilgileri ve istatistikleri için ayrı tablolar
- Otomatik temizleme özelliği ile eski kayıtların otomatik silinmesi
- Verilerin yedeklenmesi ve taşınması için kolay yapı

## 🌐 Uyumluluk

Eklenti aşağıdaki sistemlerle tam uyumludur:
- **Sunucu Yazılımları**: Spigot, Paper, Purpur, Bukkit
- **Diğer Eklentiler**: PlaceholderAPI, LuckPerms, DiscordSRV, Vault
- **Veritabanları**: MySQL, MariaDB, SQLite
- **Chat Eklentileri**: EssentialsChat, VentureChat, HeroChat ve diğer popüler chat eklentileriyle uyumlu çalışır

## 📈 Performans Etkileri

- **CPU Kullanımı**: Minimal (asenkron istek ve işlemler)
- **Bellek Kullanımı**: ~5-10MB (ayarlara bağlı olarak)
- **Ağ Trafiği**: Düşük (sadece tespit durumlarında API çağrısı)
- **Veritabanı Yükü**: Düşük (optimize edilmiş sorgular)

## 📚 Yapılandırma Seçenekleri

Kapsamlı `config.yml` dosyası ile eklentinin her yönünü özelleştirebilirsiniz:

- API ayarları (URL, timeout, model seçimi)
- Küfür tespit seviye eşikleri
- Discord webhook entegrasyonu
- Veritabanı bağlantı ayarları
- Mesaj özelleştirme
- Komut tetikleyicileri
- Anti-spam parametreleri
- Log ve raporlama seçenekleri

## 🔄 Güncellemeler ve Destek

- **Düzenli Güncellemeler**: Yeni özellikler ve tespit algoritmaları sürekli olarak eklenmektedir
- **Teknik Destek**: Özel destek kanalları aracılığıyla hızlı yardım
- **Dokümantasyon**: Detaylı kullanım kılavuzu ve SSS
- **Özelleştirme**: İsteğe bağlı özel geliştirmeler ve entegrasyonlar

## 👥 Katkıda Bulunanlar
- **KynuxCloud** - Ana Geliştirici
- **TurkishAI Team** - Yapay Zeka Modeli Eğitimi
- **Community Contributors** - Eklenti Testleri ve Geribildirim

## ⭐ Neden Bu Eklenti?

- **Türkçe Odaklı**: Özellikle Türkçe dil yapısı ve küfür kalıpları için tasarlanmıştır
- **Minimum Performans Etkisi**: Asenkron işlemler sayesinde sunucu performansını etkilemez
- **Sürekli Güncellenen**: Yeni tespit yöntemleri ve korumalar düzenli olarak eklenir
- **Tam Entegrasyon**: PlaceholderAPI, Discord ve veritabanı sistemleri ile tam uyumlu

---

## 🔍 Yapay Zeka Modelinin Çalışma Prensibi

Eklentimizin kullandığı yapay zeka modeli, özellikle Türkçe dil yapısına uygun olarak geliştirilmiş olup, şu özelliklere sahiptir:

1. **Bağlam Analizi**: Sadece tek kelimelere değil, cümle yapısına ve mesajın bağlamına bakarak küfür tespiti yapar
2. **Karakter Değiştirme Tespiti**: "a" yerine "@", "o" yerine "0" gibi karakterleri tanır
3. **Kelime Bölme Tespiti**: Araya boşluk veya özel karakter eklenerek bölünmüş küfürleri tespit eder
4. **Fonetik Benzerlik**: Sesli okunduğunda küfüre benzeyen ifadeleri tespit eder
5. **Dil Öğrenimi**: Zaman içinde yeni kalıpları ve kaçış tekniklerini öğrenir

## 📊 Performans ve İstatistik Örnekleri

```
Sunucu: SurvivalTR
Dönem: Ocak 2025
Toplam Mesaj: 145,782
Tespit Edilen Küfür: 2,834 (%1.94)
En Sık Kategori: Hakaret (%42)
Ortalama Şiddet Seviyesi: 3.2/5
En Aktif Saat: 21:00-23:00
```

## 📋 Kurulum Sonrası Kontrol Listesi

- [ ] API bağlantısını test edin (`/tpd test` komutu ile)
- [ ] Discord webhook bağlantısını doğrulayın
- [ ] Veritabanı bağlantısını kontrol edin
- [ ] İzinleri uygun şekilde ayarlayın
- [ ] En az bir admin hesabına bildirim izni verin
- [ ] Seviye bazlı komutları kendi sunucunuza göre özelleştirin
- [ ] Mesaj şablonlarını güncelleyin
- [ ] Test mesajlarıyla sistemin çalışmasını kontrol edin

---

**Geliştirici**: KynuxCloud  
**Sürüm**: 1.0.0  
**Lisans**: Tüm hakları saklıdır.  
**İletişim**: support@kynux.cloud
