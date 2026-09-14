package com.example.bandlink.service;
import com.example.bandlink.dto.ProfileUpdateRequest;
import com.example.bandlink.entity.User;
import com.example.bandlink.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.NoSuchElementException;
import java.util.Set;
@Service public class ProfileService {
 private final UserRepository users; private final PartRepository parts; private final GenreRepository genres; private final StanceRepository stances; private final PrefectureRepository prefectures;
 public ProfileService(UserRepository u, PartRepository p, GenreRepository g, StanceRepository s, PrefectureRepository pr){users=u;parts=p;genres=g;stances=s;prefectures=pr;}
// SEC-003: a suspended account's profile was still writable through this endpoint - nothing
// here or in ProfileController checked isActive() the way MessageService/PostService already do
// for DM sending and post creation, so "投稿・メッセージ送信ができません" for a suspended user
// was true everywhere except their own profile. active() matches that established rejection
// (409, RuleViolationException -> ApiExceptionHandler) rather than inventing a new shape.
 User active(Long id){User u=users.findById(id).orElseThrow(()->new RuleViolationException("ユーザーが見つかりません"));if(!u.isActive())throw new RuleViolationException("現在この操作は利用できません");return u;}
 public void requireActive(Long id){ active(id); }
 public static class RuleViolationException extends RuntimeException{public RuleViolationException(String m){super(m);}}
@Transactional public User update(Long id, ProfileUpdateRequest r){ User u=active(id); if(r.username()!=null)u.setUsername(r.username().trim());u.setGender(r.gender());u.setBio(r.bio());u.setAge(r.age());u.setExperienceYears(r.experienceYears());u.setVideoUrl(r.videoUrl());u.setYoutubeUrl(r.youtubeUrl());u.setTiktokUrl(r.tiktokUrl());u.setSoundcloudUrl(r.soundcloudUrl());u.setSpotifyUrl(r.spotifyUrl());u.setAppleMusicUrl(r.appleMusicUrl());if(r.partIds()!=null){var x=parts.findAllById(r.partIds());if(x.size()!=r.partIds().size())throw new IllegalArgumentException("パートが不正です");u.getParts().clear();u.getParts().addAll(x);}if(r.genreIds()!=null){var x=genres.findAllById(r.genreIds());if(x.size()!=r.genreIds().size())throw new IllegalArgumentException("ジャンルが不正です");u.getGenres().clear();u.getGenres().addAll(x);}if(r.stanceIds()!=null){var x=stances.findAllById(r.stanceIds());if(x.size()!=r.stanceIds().size())throw new IllegalArgumentException("スタンスが不正です");u.getStances().clear();u.getStances().addAll(x);}if(r.prefectureIds()!=null){if(r.prefectureIds().size()>3)throw new IllegalArgumentException("都道府県は3つまでです");var x=prefectures.findAllById(r.prefectureIds());if(x.size()!=r.prefectureIds().size())throw new IllegalArgumentException("都道府県が不正です");u.getPrefectures().clear();u.getPrefectures().addAll(x);}return u; }
 public User getPublic(Long id){User u=users.findById(id).orElseThrow(()->new NoSuchElementException("ユーザーが見つかりません"));if(!u.isActive())throw new NoSuchElementException("ユーザーが見つかりません");return u;}
}
