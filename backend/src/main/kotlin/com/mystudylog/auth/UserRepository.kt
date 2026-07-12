package com.mystudylog.auth

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, String>
