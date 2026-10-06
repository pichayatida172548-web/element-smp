# Element Armor / Element SMP (Fabric, Minecraft Java 26.3)

Mod ฝั่ง **เซิร์ฟเวอร์** (ผู้เล่นไม่ต้องลง mod) — ต้องมี Fabric API

## Build
ต้องใช้ JDK 25
```
gradle wrapper --gradle-version 9.5.1     # ครั้งแรกครั้งเดียว (หรือคัดลอก wrapper จาก Fabric template)
./gradlew build
```
ไฟล์ jar อยู่ที่ `build/libs/element-smp-1.0.0.jar`

## คำสั่ง
| คำสั่ง | ใคร | ทำอะไร |
|---|---|---|
| `/element list` | ทุกคน | ดูธาตุและคู่ Trim |
| `/element get [player]` | ทุกคน / OP | ดูธาตุ |
| `/element set <player> <element>` | OP | ตั้งธาตุ |
| `/element random <player>` | OP | สุ่มธาตุที่มีคนใช้น้อยสุด |
| `/element clear <player>` | OP | ลบธาตุ |
| `/element reload` | OP | โหลด config ใหม่ |

## แก้คู่ Trim
แก้ `config/element_smp.json` แล้วพิมพ์ `/element reload` (ใช้ id ของ pattern/material จาก vanilla หรือ datapack ก็ได้)

## หลักการรักษาข้อมูลเกราะ
- ไม่สร้างไอเทมใหม่ — แก้ component `minecraft:trim` บน ItemStack ตัวเดิมที่ใส่อยู่
- Enchant, ชื่อ, durability, material, component อื่นไม่ถูกแตะ
- ถ้าเกราะมี Trim เดิม จะจำไว้ใน `custom_data.element_smp` ชั่วคราว แล้วคืนให้เมื่อถอด / ดรอป / ออกจากเกม / เซิร์ฟเวอร์ปิด
