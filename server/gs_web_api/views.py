from rest_framework import viewsets
from .models import Post, Comment, Flag
from .serializers import PostSerializer, CommentSerializer, FlagSerializer
from rest_framework import status
from rest_framework.response import Response
from django.utils import timezone


class PostViewSet(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer

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

    def destroy(self, request, pk=None):
        # make sure that the 'client_secret' has been provided
        # TODO: replace with custom permission
        post = self.get_object()
        if 'HTTP_CLIENTSECRET' in request.META and str(request.META['HTTP_CLIENTSECRET']) == str(post.client_secret):
            return viewsets.ModelViewSet.destroy(self, request, pk)
        else:
            return Response(status=status.HTTP_403_FORBIDDEN)


class CommentViewSet(viewsets.ModelViewSet):
    queryset = Comment.objects.all()
    serializer_class = CommentSerializer


class FlagViewSet(viewsets.ModelViewSet):
    queryset = Flag.objects.all()
    serializer_class = FlagSerializer
