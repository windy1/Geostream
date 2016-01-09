from rest_framework import viewsets
from .models import Post, Comment, Flag
from .serializers import PostSerializer, CommentSerializer, FlagSerializer
from rest_framework import status, permissions
from rest_framework.decorators import list_route
from rest_framework.response import Response
from django.utils import timezone


class ClientSecretPermission(permissions.BasePermission):
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


def inject_client_secret(serializer):
    """
    Injects a models 'client_secret' field into a request response. Client secrets are only returned on the initial
    creation of an object.
    """
    if (serializer.is_valid()):
        obj = serializer.save()
        data = serializer.data
        data['client_secret'] = obj.client_secret
        return Response(data)
    else:
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


def delete_expired_posts():
    posts = Post.objects.all()
    for post in posts:
        now = timezone.now()
        dt = now - post.created  # timedelta between the creation date of the post and now
        mins = divmod(dt.total_seconds(), 60)  # (minutes, seconds)
        hours = mins[0] / 60
        if hours >= post.lifetime:
            post.delete()


class PostViewSet(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer
    http_method_names = ['get', 'post', 'head', 'delete']
    permission_classes = (ClientSecretPermission, )

    @list_route()
    def range(self, request):
        """
        Returns a list view of the posts within a specified lat/lng range.
        """
        data = request.query_params
        # make range parameters are present
        if 'fromLat' not in data or 'toLat' not in data or 'fromLng' not in data or 'toLng' not in data:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        fromLat = float(data['fromLat'])
        toLat = float(data['toLat'])
        fromLng = float(data['fromLng'])
        toLng = float(data['toLng'])

        # find posts that are within the range
        delete_expired_posts()
        posts = Post.objects.all()
        inRange = []
        for post in posts:
            if post.lat >= fromLat and post.lat <= toLat and post.lng >= fromLng and post.lng <= toLng:
                inRange.append(post)

        # return list of posts
        serializer = self.get_serializer(inRange, many=True)
        return Response(serializer.data)

    def list(self, request):
        delete_expired_posts()
        return viewsets.ModelViewSet.list(self, request)  # after deleting old posts, pass to super method

    def create(self, request):
        return inject_client_secret(PostSerializer(data=request.data))


class CommentViewSet(viewsets.ModelViewSet):
    queryset = Comment.objects.all()
    serializer_class = CommentSerializer
    http_method_names = ['get', 'post', 'head', 'delete']
    permission_classes = (ClientSecretPermission, )

    def create(self, request):
        return inject_client_secret(CommentSerializer(data=request.data))


class FlagViewSet(viewsets.ModelViewSet):
    queryset = Flag.objects.all()
    serializer_class = FlagSerializer
    http_method_names = ['get', 'post', 'head']
