package com.paseto.repository;

import com.paseto.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByActiveOrderByDisplayOrderAsc(Boolean active);

    List<Banner> findAllByOrderByDisplayOrderAsc();
}
