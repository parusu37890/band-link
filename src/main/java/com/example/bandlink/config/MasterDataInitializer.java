package com.example.bandlink.config;

import com.example.bandlink.entity.*;
import com.example.bandlink.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.*;

/** Adds approved options without changing existing identifiers or user data. */
@Component
public class MasterDataInitializer implements ApplicationRunner {
    private final PartRepository parts;
    private final GenreRepository genres;
    private final StanceRepository stances;
    private final PrefectureRepository prefectures;
    public MasterDataInitializer(PartRepository p, GenreRepository g, StanceRepository s, PrefectureRepository pr) {
        parts = p; genres = g; stances = s; prefectures = pr;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        add("ボーカル,ギター,ベース,ドラム,キーボード,作詞作曲", parts.findAll(), Part::getName, Part::new, parts::save);
        add("ポップス,邦ロック,洋ロック,アニソン,ボカロ,ハードロック／メタル,パンク／メロコア,ジャズ,ブルース,ファンク／ソウル,R&B,フォーク／カントリー,クラシック", genres.findAll(), Genre::getName, Genre::new, genres::save);
        add("趣味で楽しみたい,趣味でも本格的に取り組みたい,プロを目指したい,プロとして活動中", stances.findAll(), Stance::getName, Stance::new, stances::save);
        add("北海道,青森県,岩手県,宮城県,秋田県,山形県,福島県,茨城県,栃木県,群馬県,埼玉県,千葉県,東京都,神奈川県,新潟県,富山県,石川県,福井県,山梨県,長野県,岐阜県,静岡県,愛知県,三重県,滋賀県,京都府,大阪府,兵庫県,奈良県,和歌山県,鳥取県,島根県,岡山県,広島県,山口県,徳島県,香川県,愛媛県,高知県,福岡県,佐賀県,長崎県,熊本県,大分県,宮崎県,鹿児島県,沖縄県", prefectures.findAll(), Prefecture::getName, Prefecture::new, prefectures::save);
    }
    private <T> void add(String names, List<T> current, Function<T, String> name, BiFunction<String, Integer, T> factory, Consumer<T> save) {
        Set<String> present = new HashSet<>(); current.forEach(v -> present.add(name.apply(v)));
        String[] labels = names.split(",");
        for (int i = 0; i < labels.length; i++) if (present.add(labels[i])) save.accept(factory.apply(labels[i], i));
    }
}
