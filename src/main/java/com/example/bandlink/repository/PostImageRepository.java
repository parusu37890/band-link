package com.example.bandlink.repository;

import com.example.bandlink.entity.PostImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    @Query("select image from PostImage image where image.post.id = :postId order by image.sortOrder asc")
    List<PostImage> findByPostIdOrderBySortOrderAsc(@Param("postId") Long postId);

    @Query("select count(image) from PostImage image where image.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);
}
