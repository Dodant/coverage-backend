package com.bookcover.archive.service.impl

import com.bookcover.archive.domain.user.User
import com.bookcover.archive.domain.user.UserTitle
import com.bookcover.archive.exception.ResourceNotFoundException
import com.bookcover.archive.repository.user.UserRepository
import com.bookcover.archive.service.ContributionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Implementation of ContributionService.
 *
 * Requirements:
 * - 7.5: Auto-assign Contributor title when user registers 10+ books
 * - 7.6: Track contribution count for each user
 */
@Service
@Transactional
class ContributionServiceImpl(
    private val userRepository: UserRepository
) : ContributionService {

    override fun recordContribution(userId: UUID): User {
        val user = userRepository.findById(userId).orElseThrow {
            ResourceNotFoundException("User", userId)
        }

        // Increment contribution count and update title if eligible
        user.incrementContributionCount()

        return userRepository.save(user)
    }

    @Transactional(readOnly = true)
    override fun getContributionCount(userId: UUID): Int {
        val user = userRepository.findById(userId).orElseThrow {
            ResourceNotFoundException("User", userId)
        }

        return user.contributionCount
    }

    @Transactional(readOnly = true)
    override fun isEligibleForContributorTitle(userId: UUID): Boolean {
        val user = userRepository.findById(userId).orElseThrow {
            ResourceNotFoundException("User", userId)
        }

        return user.contributionCount >= User.CONTRIBUTOR_THRESHOLD
    }

    override fun decrementContribution(userId: UUID): User {
        val user = userRepository.findById(userId).orElseThrow {
            ResourceNotFoundException("User", userId)
        }

        if (user.contributionCount > 0) {
            user.contributionCount--
        }

        // Note: We don't demote the title when contribution count drops below threshold
        // Once earned, the Contributor title is retained

        return userRepository.save(user)
    }
}
