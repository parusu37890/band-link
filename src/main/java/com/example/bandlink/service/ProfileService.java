package com.example.bandlink.service;
import com.example.bandlink.dto.ProfileUpdateRequest;
import com.example.bandlink.entity.User;
import com.example.bandlink.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
@Service public class ProfileService {
 private final UserRepository users; private final PartRepository parts; private final GenreRepository genres; private final StanceRepository stances; private final PrefectureRepository prefectures;
 public ProfileService(UserRepository u, PartRepository p, GenreRepository g, StanceRepository s, PrefectureRepository pr){users=u;parts=p;genres=g;stances=s;prefectures=pr;}
 @Transactional public User update(Long id, ProfileUpdateRequest r){ User u=users.findById(id).orElseThrow(()->new IllegalArgumentException("ユーザーが見つかりません")); if(r.username()!=null)u.setUsername(r.username().trim());u.setGender(r.gender());u.setBio(r.bio());u.setAge(r.age());u.setExperienceYears(r.experienceYears());u.setVideoUrl(r.videoUrl());u.setYoutubeUrl(r.youtubeUrl());u.setTiktokUrl(r.tiktokUrl());u.setSoundcloudUrl(r.soundcloudUrl());u.setSpotifyUrl(r.spotifyUrl());u.setAppleMusicUrl(r.appleMusicUrl());if(r.partIds()!=null){u.getParts().clear();u.getParts().addAll(parts.findAllById(r.partIds()));}if(r.genreIds()!=null){u.getGenres().clear();u.getGenres().addAll(genres.findAllById(r.genreIds()));}if(r.stanceIds()!=null){u.getStances().clear();u.getStances().addAll(stances.findAllById(r.stanceIds()));}if(r.prefectureIds()!=null){if(r.prefectureIds().size()>3)throw new IllegalArgumentException("都道府県は3つまでです");u.getPrefectures().clear();u.getPrefectures().addAll(prefectures.findAllById(r.prefectureIds()));}return u; }
 public User getPublic(Long id){User u=users.findById(id).orElseThrow(()->new IllegalArgumentException("ユーザーが見つかりません"));if(!u.isActive())throw new IllegalArgumentException("ユーザーが見つかりません");return u;}
}
