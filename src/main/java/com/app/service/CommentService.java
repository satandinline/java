package com.app.service;

import com.app.entity.CommentLike;
import com.app.entity.CommentReply;
import com.app.entity.ReplyLike;
import com.app.entity.User;
import com.app.entity.UserComment;
import com.app.repository.CommentLikeRepository;
import com.app.repository.CommentReplyRepository;
import com.app.repository.ReplyLikeRepository;
import com.app.repository.UserCommentRepository;
import com.app.repository.UserRepository;
import com.app.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommentService {
    @Autowired
    private UserCommentRepository commentRepository;
    
    @Autowired
    private CommentReplyRepository replyRepository;
    
    @Autowired
    private CommentLikeRepository commentLikeRepository;
    
    @Autowired
    private ReplyLikeRepository replyLikeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationService notificationService;

    public Map<String, Object> getComments(Long resourceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<UserComment> comments = commentRepository.findApprovedCommentsByResourceId(
                resourceId, UserComment.CommentStatus.approved);
            
            List<Map<String, Object>> commentList = comments.stream().map(comment -> {
                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("id", comment.getId());
                commentMap.put("resource_id", comment.getResourceId());
                commentMap.put("user_id", comment.getUserId());
                commentMap.put("comment_content", comment.getCommentContent());
                commentMap.put("created_at", comment.getCreatedAt());
                
                // 获取用户信息
                Optional<User> userOpt = userRepository.findById(comment.getUserId());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    commentMap.put("account", user.getAccount());
                    commentMap.put("nickname", user.getNickname());
                    commentMap.put("avatar_path", user.getAvatarPath());
                }
                
                // 获取点赞数
                long likeCount = commentLikeRepository.countByCommentId(comment.getId());
                commentMap.put("like_count", likeCount);
                
                // 获取回复
                List<CommentReply> replies = replyRepository.findByCommentIdOrderByCreatedAtAsc(comment.getId());
                List<Map<String, Object>> replyList = replies.stream().map(reply -> {
                    Map<String, Object> replyMap = new HashMap<>();
                    replyMap.put("id", reply.getId());
                    replyMap.put("comment_id", reply.getCommentId());
                    replyMap.put("reply_user_id", reply.getReplyUserId());
                    replyMap.put("reply_content", reply.getReplyContent());
                    replyMap.put("created_at", reply.getCreatedAt());
                    
                    // 获取回复用户信息
                    Optional<User> replyUserOpt = userRepository.findById(reply.getReplyUserId());
                    if (replyUserOpt.isPresent()) {
                        User replyUser = replyUserOpt.get();
                        replyMap.put("account", replyUser.getAccount());
                        replyMap.put("nickname", replyUser.getNickname());
                        replyMap.put("avatar_path", replyUser.getAvatarPath());
                    }
                    
                    // 获取回复点赞数
                    long replyLikeCount = replyLikeRepository.countByReplyId(reply.getId());
                    replyMap.put("like_count", replyLikeCount);
                    return replyMap;
                }).collect(Collectors.toList());
                commentMap.put("replies", replyList);
                
                return commentMap;
            }).collect(Collectors.toList());
            
            result.put("success", true);
            result.put("comments", commentList);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取评论失败：" + e.getMessage());
        }
        return result;
    }

    @Transactional
    public Map<String, Object> createComment(Long resourceId, Long userId, String commentContent) {
        Map<String, Object> result = new HashMap<>();
        
        if (commentContent == null || commentContent.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "评论内容不能为空");
            return result;
        }
        
        try {
            UserComment comment = new UserComment();
            comment.setResourceId(resourceId);
            comment.setUserId(userId);
            comment.setCommentContent(commentContent.trim());
            comment.setCommentStatus(UserComment.CommentStatus.approved);
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            
            comment = commentRepository.save(comment);
            
            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("id", comment.getId());
            commentMap.put("resource_id", comment.getResourceId());
            commentMap.put("user_id", comment.getUserId());
            commentMap.put("comment_content", comment.getCommentContent());
            commentMap.put("created_at", comment.getCreatedAt());
            commentMap.put("like_count", 0);
            commentMap.put("replies", List.of());
            
            result.put("success", true);
            result.put("comment", commentMap);
            result.put("message", "评论发布成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建评论失败：" + e.getMessage());
        }
        
        return result;
    }

    @Transactional
    public Map<String, Object> addReply(Long commentId, Long userId, String replyContent) {
        Map<String, Object> result = new HashMap<>();
        
        if (replyContent == null || replyContent.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "回复内容不能为空");
            return result;
        }
        
        try {
            // 获取评论信息，用于发送通知
            Optional<UserComment> commentOpt = commentRepository.findById(commentId);
            if (commentOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }
            
            UserComment comment = commentOpt.get();
            
            CommentReply reply = new CommentReply();
            reply.setCommentId(commentId);
            reply.setReplyUserId(userId);
            reply.setReplyContent(replyContent.trim());
            reply.setCreatedAt(LocalDateTime.now());
            
            reply = replyRepository.save(reply);
            
            // 发送通知给评论作者（如果回复者不是评论作者）
            if (!comment.getUserId().equals(userId)) {
                // 获取回复用户信息
                Optional<User> replyUserOpt = userRepository.findById(userId);
                String replyUserName = "用户";
                if (replyUserOpt.isPresent()) {
                    User replyUser = replyUserOpt.get();
                    replyUserName = replyUser.getNickname() != null && !replyUser.getNickname().isEmpty() 
                        ? replyUser.getNickname() : replyUser.getAccount();
                }
                
                // 创建通知
                notificationService.createNotification(
                    comment.getUserId(),
                    "reply",
                    replyUserName + "回复了您的评论",
                    commentId
                );
            }
            
            Map<String, Object> replyMap = new HashMap<>();
            replyMap.put("id", reply.getId());
            replyMap.put("comment_id", reply.getCommentId());
            replyMap.put("reply_user_id", reply.getReplyUserId());
            replyMap.put("reply_content", reply.getReplyContent());
            replyMap.put("created_at", reply.getCreatedAt());
            replyMap.put("like_count", 0);
            
            result.put("success", true);
            result.put("reply", replyMap);
            result.put("message", "回复添加成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "添加回复失败：" + e.getMessage());
        }
        
        return result;
    }
    
    public Map<String, Object> getComment(Long commentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Optional<UserComment> commentOpt = commentRepository.findById(commentId);
            if (commentOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }
            
            UserComment comment = commentOpt.get();
            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("id", comment.getId());
            commentMap.put("resource_id", comment.getResourceId());
            commentMap.put("user_id", comment.getUserId());
            commentMap.put("comment_content", comment.getCommentContent());
            commentMap.put("created_at", comment.getCreatedAt());
            
            // 获取用户信息
            Optional<User> userOpt = userRepository.findById(comment.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                commentMap.put("account", user.getAccount());
                commentMap.put("nickname", user.getNickname());
                commentMap.put("avatar_path", user.getAvatarPath());
            }
            
            // 获取点赞数
            long likeCount = commentLikeRepository.countByCommentId(comment.getId());
            commentMap.put("like_count", likeCount);
            
            result.put("success", true);
            result.put("comment", commentMap);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取评论失败：" + e.getMessage());
        }
        return result;
    }
    
    public Map<String, Object> getCommentResourceId(Long commentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Optional<UserComment> commentOpt = commentRepository.findById(commentId);
            if (commentOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }
            
            UserComment comment = commentOpt.get();
            result.put("success", true);
            result.put("resource_id", comment.getResourceId());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取资源ID失败：" + e.getMessage());
        }
        return result;
    }
    
    @Transactional
    public Map<String, Object> likeComment(Long commentId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);
            
            String action;
            if (existingLike.isPresent()) {
                // 已点赞，取消点赞
                commentLikeRepository.delete(existingLike.get());
                action = "unliked";
            } else {
                // 未点赞，添加点赞
                CommentLike like = new CommentLike();
                like.setCommentId(commentId);
                like.setUserId(userId);
                commentLikeRepository.save(like);
                action = "liked";
                
                // 发送通知给评论作者
                Optional<UserComment> commentOpt = commentRepository.findById(commentId);
                if (commentOpt.isPresent()) {
                    UserComment comment = commentOpt.get();
                    if (!comment.getUserId().equals(userId)) {
                        // 获取点赞用户信息
                        Optional<User> likerOpt = userRepository.findById(userId);
                        String likerName = "用户";
                        if (likerOpt.isPresent()) {
                            User liker = likerOpt.get();
                            likerName = liker.getNickname() != null && !liker.getNickname().isEmpty() 
                                ? liker.getNickname() : liker.getAccount();
                        }
                        
                        // 创建通知
                        notificationService.createNotification(
                            comment.getUserId(),
                            "like",
                            likerName + "点赞了您的评论",
                            commentId
                        );
                    }
                }
            }
            
            // 获取当前点赞数
            long likeCount = commentLikeRepository.countByCommentId(commentId);
            
            result.put("success", true);
            result.put("action", action);
            result.put("like_count", likeCount);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "点赞失败：" + e.getMessage());
        }
        return result;
    }
    
    @Transactional
    public Map<String, Object> likeReply(Long replyId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Optional<ReplyLike> existingLike = replyLikeRepository.findByReplyIdAndUserId(replyId, userId);
            
            String action;
            if (existingLike.isPresent()) {
                // 已点赞，取消点赞
                replyLikeRepository.delete(existingLike.get());
                action = "unliked";
            } else {
                // 未点赞，添加点赞
                ReplyLike like = new ReplyLike();
                like.setReplyId(replyId);
                like.setUserId(userId);
                replyLikeRepository.save(like);
                action = "liked";
            }
            
            // 获取当前点赞数
            long likeCount = replyLikeRepository.countByReplyId(replyId);
            
            result.put("success", true);
            result.put("action", action);
            result.put("like_count", likeCount);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "点赞失败：" + e.getMessage());
        }
        return result;
    }
}

