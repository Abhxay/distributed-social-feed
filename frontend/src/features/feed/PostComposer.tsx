import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { useCreatePostMutation, useUploadPostImageMutation } from '../../api/apiSlice';

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

export function PostComposer() {
  const [headline, setHeadline] = useState('');
  const [body, setBody] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [imageError, setImageError] = useState('');
  const [statusMessage, setStatusMessage] = useState('');
  const [createPost, { isLoading: isPosting }] = useCreatePostMutation();
  const [uploadPostImage, { isLoading: isUploading }] = useUploadPostImageMutation();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Revoke on change/unmount so each previous object URL doesn't leak.
  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  const clearImage = () => {
    setPreviewUrl((current) => {
      if (current) URL.revokeObjectURL(current);
      return null;
    });
    setFile(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0] ?? null;
    setImageError('');
    if (!selected) {
      clearImage();
      return;
    }
    if (!selected.type.startsWith('image/')) {
      setImageError('Please choose an image file.');
      clearImage();
      return;
    }
    if (selected.size > MAX_IMAGE_BYTES) {
      setImageError('Image must be 5MB or smaller.');
      clearImage();
      return;
    }
    setFile(selected);
    setPreviewUrl((current) => {
      if (current) URL.revokeObjectURL(current);
      return URL.createObjectURL(selected);
    });
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!headline.trim() || !body.trim()) return;
    setStatusMessage('');

    const newPost = await createPost({ headline, body }).unwrap();

    if (file) {
      try {
        await uploadPostImage({ postId: newPost.postId, file }).unwrap();
      } catch {
        setStatusMessage('Post published, but the image failed to upload.');
      }
    }

    setHeadline('');
    setBody('');
    clearImage();
  };

  return (
    <form onSubmit={handleSubmit} className="post-composer">
      <label htmlFor="post-headline">Headline</label>
      <input
        id="post-headline"
        maxLength={150}
        placeholder="Give it a headline"
        value={headline}
        onChange={(e) => setHeadline(e.target.value)}
      />
      <span className="muted composer-counter">{headline.length}/150</span>

      <label htmlFor="post-body">New post</label>
      <textarea
        id="post-body"
        rows={6}
        maxLength={2000}
        placeholder="Share something: a thought, a story, a whole paragraph if you want."
        value={body}
        onChange={(e) => setBody(e.target.value)}
      />
      <span className="muted composer-counter">{body.length}/2000</span>

      <label htmlFor="post-image">Image (optional)</label>
      <input id="post-image" ref={fileInputRef} type="file" accept="image/*" onChange={handleFileChange} />
      {imageError && <span className="composer-error">{imageError}</span>}
      {previewUrl && (
        <div className="composer-preview">
          <img src={previewUrl} alt="Selected upload preview" />
          <button type="button" onClick={clearImage}>
            Remove image
          </button>
        </div>
      )}

      {statusMessage && <span className="composer-status">{statusMessage}</span>}

      <button type="submit" disabled={isPosting || isUploading || !headline.trim() || !body.trim()}>
        Post
      </button>
    </form>
  );
}
