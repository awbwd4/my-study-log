package com.mystudylog.academy.school

import org.springframework.data.jpa.repository.JpaRepository

interface SchoolRepository : JpaRepository<School, Long>
