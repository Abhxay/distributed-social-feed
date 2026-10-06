package com.showcase.feed.comments;

import com.showcase.feed.explore.ExploreRanking;
import com.showcase.feed.posts.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ExploreRanking exploreRanking;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository,
                           ExploreRanking exploreRanking) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.exploreRanking = exploreRanking;
    }

    @Transactional
    public Comment createComment(UUID authorId, UUID postId, String body) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(authorId);
        comment.setBody(body);
        commentRepository.save(comment);

        postRepository.findById(postId).ifPresent(post -> {
            exploreRanking.creditComment(post.getAuthorId());
            post.setCommentCount(post.getCommentCount() + 1);
            postRepository.save(post);
        });
        return comment;
    }
}
