from rest_framework import viewsets
from .models import Post, Comment, Flag
from .serializers import PostSerializer, CommentSerializer, FlagSerializer
from rest_framework import status, permissions
from rest_framework.response import Response
from django.utils import timezone


class PostDeletePermission(permissions.BasePermission):
    """
    Permission that determines whether a Post can be deleted by a client. In order to delete a post the client must
    provide a ClientSecret header that matches the client_secret field in the Post object that they are trying to
    delete.
    """
    META_KEY = 'HTTP_CLIENTSECRET'

    def has_object_permission(self, request, view, obj):
        if request.method != 'DELETE':
            return True  # this permission only handles deletions

        if self.META_KEY not in request.META:
            return False  # no client secret provided
        client_secret = request.META[self.META_KEY]

        return str(obj.client_secret) == str(client_secret)  # check if the client secrets match


class PostViewSet(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer
    http_method_names = ['get', 'post', 'head', 'delete']
    permission_classes = (PostDeletePermission, )

    def list(self, request):
        # delete expired posts
        posts = Post.objects.all()
        for post in posts:
            now = timezone.now()
            dt = now - post.created  # timedelta between the creation date of the post and now
            mins = divmod(dt.total_seconds(), 60)  # (minutes, seconds)
            hours = mins[0] / 60
            if hours >= post.lifetime:
                post.delete()
        return viewsets.ModelViewSet.list(self, request)  # after deleting old posts, pass to super method

    def create(self, request):
        # include the 'client_secret' within the response only on creation
        serializer = PostSerializer(data=request.data)
        if (serializer.is_valid()):
            post = serializer.save()
            data = serializer.data
            data['client_secret'] = post.client_secret
            return Response(data)
        else:
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class CommentViewSet(viewsets.ModelViewSet):
    queryset = Comment.objects.all()
    serializer_class = CommentSerializer


class FlagViewSet(viewsets.ModelViewSet):
    queryset = Flag.objects.all()
    serializer_class = FlagSerializer
