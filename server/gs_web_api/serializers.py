from rest_framework import serializers
from .models import Post, Comment, Flag


class FlagSerializer(serializers.ModelSerializer):
    class Meta:
        model = Flag
        fields = ('id', 'resource_type', 'resource_id', 'created', 'reason',)


class CommentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Comment
        fields = ('id', 'post', 'created', 'content',)


class NestedCommentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Comment
        fields = ('created', 'content')


class PostSerializer(serializers.ModelSerializer):
    comments = NestedCommentSerializer(many=True, read_only=True)

    class Meta:
        model = Post
        fields = ('id', 'created', 'lat', 'lng', 'lifetime', 'media_file', 'comments')
